package pk.qist.customer.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only an encrypted bearer token is persisted. Passwords and financial records are never saved here. */
internal class EncryptedCustomerSession(context: Context) : CustomerSessionStore {
    private val appContext = context.applicationContext
    private val alias = "qist.customer.session.v1"
    private fun preferences() = appContext.getSharedPreferences("customer_session", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        val encoded = preferences().getString("token", null) ?: return@withContext null
        try {
            val parts = encoded.split(':')
            if (parts.size != 2) throw IllegalArgumentException("Invalid session encoding")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: GeneralSecurityException) { clearSavedToken(); null }
        catch (_: IllegalArgumentException) { clearSavedToken(); null }
    }
    private fun clearSavedToken() {
        if (!preferences().edit().clear().commit()) throw IOException("Could not clear the saved session. Please try again.")
    }
    override suspend fun write(token: String?) = withContext(Dispatchers.IO) {
        val editor = preferences().edit()
        if (token == null) editor.clear() else {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val encrypted = Base64.encodeToString(cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            editor.putString("token", "$iv:$encrypted")
        }
        if (!editor.commit()) throw IOException("Could not save the sign-in session. Please try again.")
    }
}
