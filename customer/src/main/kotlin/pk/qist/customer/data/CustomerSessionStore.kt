package pk.qist.customer.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.AtomicFile
import java.io.IOException

internal interface SessionStore {
    suspend fun save(token: String, expiresAt: String)
    suspend fun read(): String?
    suspend fun clear()
}

/** Keystore-bound ciphertext in noBackupFilesDir; never store passwords or backup session tokens. */
internal class CustomerSessionStore(context: Context) : SessionStore {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "customer-session"))
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("qist-customer-session", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("qist-customer-session", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override suspend fun save(token: String, expiresAt: String) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val content = cipher.iv + cipher.doFinal("$expiresAt\n$token".toByteArray(Charsets.UTF_8))
        val stream = file.startWrite()
        try { stream.write(content); file.finishWrite(stream) } catch (e: IOException) { file.failWrite(stream); throw e }
    }
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) return@withContext null
        try {
            val bytes = file.readFully()
            require(bytes.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            }
            val parts = cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8).split('\n', limit = 2)
            if (parts.size == 2 && (SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(parts[0])?.time ?: 0L) > System.currentTimeMillis()) parts[1] else { file.delete(); null }
        } catch (_: GeneralSecurityException) { file.delete(); null }
        catch (_: IllegalArgumentException) { file.delete(); null }
        catch (_: java.text.ParseException) { file.delete(); null }
        catch (_: IOException) { file.delete(); null }
    }
    override suspend fun clear() = withContext(Dispatchers.IO) { file.delete() }
}
