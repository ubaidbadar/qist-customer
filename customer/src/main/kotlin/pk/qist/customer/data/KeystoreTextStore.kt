package pk.qist.customer.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** AES-GCM with fresh IVs, atomic writes and a non-exportable key; compatible with existing sessions. */
internal class KeystoreTextStore(context: Context) : SecureTextStore {
    private val directory = context.applicationContext.noBackupFilesDir
    private fun file(name: String): AtomicFile {
        require(name == "customer-session" || name == "customer-overview")
        return AtomicFile(File(directory, name))
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("qist-customer-session", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("qist-customer-session", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override suspend fun write(name: String, value: String) = withContext(Dispatchers.IO) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.size > 2 * 1024 * 1024) throw IOException("Account snapshot is too large to save")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val content = cipher.iv + cipher.doFinal(bytes)
        val target = file(name)
        val stream = target.startWrite()
        try { stream.write(content); target.finishWrite(stream) }
        catch (error: IOException) { target.failWrite(stream); throw error }
    }
    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        val target = file(name)
        if (!target.baseFile.exists()) return@withContext null
        try {
            require(target.baseFile.length() <= 2 * 1024 * 1024 + 64)
            val bytes = target.readFully()
            require(bytes.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            }
            cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
        } catch (_: GeneralSecurityException) { target.delete(); null }
        catch (_: IllegalArgumentException) { target.delete(); null }
        catch (_: IOException) { target.delete(); null }
    }
    override suspend fun delete(name: String) = withContext(Dispatchers.IO) { file(name).delete() }
}
