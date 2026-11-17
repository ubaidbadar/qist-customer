package pk.qist.customer.documents

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.work.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import pk.qist.customer.BuildConfig
import pk.qist.customer.data.CustomerApiException
import pk.qist.customer.data.CustomerSessionStore
import pk.qist.customer.data.KeystoreTextStore

internal fun sessionScope(token: String): String = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
    .joinToString("") { "%02x".format(it) }
internal const val UPLOAD_TAG = "private-document-upload"
internal fun planUploadTag(contractId: String) = "$UPLOAD_TAG:$contractId"

/** Private, encrypted, backup-excluded copies: never persist a photo picker URI or a bearer token. */
internal class DocumentInputException(message: String) : IOException(message)

internal class PrivateUploads(context: Context) {
    private companion object { val enqueueLock = Mutex() }
    private val context = context.applicationContext
    private val directory = File(this.context.noBackupFilesDir, "document-uploads")
    fun file(id: String): File {
        require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false))
        return File(directory, id)
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("qist-private-uploads", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("qist-private-uploads", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    suspend fun enqueue(uri: Uri, contractId: String, category: String, maxBytes: Long, requestId: String?, replacesId: String?): UUID = withContext(Dispatchers.IO) {
        enqueueLock.withLock {
            val sessions = CustomerSessionStore(KeystoreTextStore(context))
            val token = sessions.read() ?: throw CustomerApiException(401, "Please sign in again.")
            directory.mkdirs()
            directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > TimeUnit.DAYS.toMillis(1) }?.forEach { it.delete() }
            if (directory.listFiles().orEmpty().size >= 3) throw DocumentInputException("Three files are already queued. Finish or cancel an upload first.")
            val id = UUID.randomUUID(); val target = file(id.toString())
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
                var size = 0L
                target.outputStream().use { output ->
                    output.write(cipher.iv)
                    CipherOutputStream(output, cipher).use { encrypted ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) { val count = input.read(buffer); if (count < 0) break; size += count
                                if (size > maxBytes.coerceAtMost(10 * 1024 * 1024)) throw DocumentInputException("Choose a smaller file.")
                                encrypted.write(buffer, 0, count)
                            }
                        } ?: throw DocumentInputException("Could not read the selected file.")
                    }
                }
                if (size == 0L) throw DocumentInputException("Choose a non-empty file.")
                if (sessions.read() != token) throw CustomerApiException(401, "The account changed. Please choose the file again.")
                val work = OneTimeWorkRequestBuilder<DocumentUploadWorker>()
                    .setId(id).addTag(UPLOAD_TAG).addTag(planUploadTag(contractId))
                    .setInputData(workDataOf("file" to id.toString(), "scope" to sessionScope(token), "contract" to contractId,
                        "category" to category, "size" to size, "request" to requestId, "replaces" to replacesId))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
                WorkManager.getInstance(context).enqueueUniqueWork(id.toString(), ExistingWorkPolicy.KEEP, work).await()
                id
            } catch (error: Throwable) { target.delete(); throw error }
        }
    }
    fun body(id: String, size: Long): RequestBody = object : RequestBody() {
        override fun contentType() = "application/octet-stream".toMediaType()
        override fun contentLength() = size
        override fun writeTo(sink: BufferedSink) {
            file(id).inputStream().use { input ->
                val iv = ByteArray(12); if (input.read(iv) != iv.size) throw IOException("The queued file is no longer available.")
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
                CipherInputStream(input, cipher).use { plain ->
                    val buffer = ByteArray(32 * 1024); var total = 0L
                    while (true) { val count = plain.read(buffer); if (count < 0) break; total += count
                        if (total > size) throw IOException("The queued file changed.")
                        sink.write(buffer, 0, count)
                    }
                    if (total != size) throw IOException("The queued file changed.")
                }
            }
        }
    }
    suspend fun cancel(id: UUID) = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelWorkById(id)
        file(id.toString()).delete()
    }
}

/** The OS can defer retries; a changed or revoked session can never complete a previous account's job. */
class DocumentUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("file") ?: return@withContext Result.failure()
        val uploads = PrivateUploads(applicationContext)
        val file = uploads.file(id)
        var keep = false
        try {
            val token = CustomerSessionStore(KeystoreTextStore(applicationContext)).read()
            if (token == null || sessionScope(token) != inputData.getString("scope") || !file.exists() || System.currentTimeMillis() - file.lastModified() > TimeUnit.DAYS.toMillis(1))
                return@withContext Result.failure(workDataOf("error" to "Upload ended. Choose the file again after signing in."))
            val api = DocumentApi(CustomerSessionStore(KeystoreTextStore(applicationContext)), BuildConfig.CUSTOMER_API_URL, inputData.getString("scope"))
            val item = api.create(inputData.getString("contract") ?: return@withContext Result.failure(),
                inputData.getString("category") ?: return@withContext Result.failure(), id, inputData.getString("request"), inputData.getString("replaces"))
            if (item.processing !in listOf("uploading", "processing", "ready")) return@withContext Result.failure(workDataOf("error" to "This upload ended. Choose the file again."))
            if (item.processing == "uploading") api.upload(item.id, uploads.body(id, inputData.getLong("size", 0)))
            Result.success(workDataOf("document" to item.id))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { keep = true; throw cancelled }
        catch (error: IOException) {
            val transient = error !is CustomerApiException || error.status == 429 || error.status >= 500
            if (transient && runAttemptCount < 5) { keep = true; Result.retry() }
            else Result.failure(workDataOf("error" to (if (error is CustomerApiException) error.message else "Upload could not finish. Please choose the file again.")))
        } catch (_: java.security.GeneralSecurityException) {
            Result.failure(workDataOf("error" to "The saved file could not be opened. Please choose it again."))
        } finally { if (!keep) file.delete() }
    }
}
