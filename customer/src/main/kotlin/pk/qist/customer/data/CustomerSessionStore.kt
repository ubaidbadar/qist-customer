package pk.qist.customer.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal interface SessionStore {
    suspend fun save(token: String, expiresAt: String)
    suspend fun read(): String?
    suspend fun clear()
}

internal class CustomerSessionStore(private val storage: SecureTextStore) : SessionStore {
    override suspend fun save(token: String, expiresAt: String) = storage.write("customer-session", "$expiresAt\n$token")
    override suspend fun read(): String? {
        val text = storage.read("customer-session") ?: return null
        val parts = text.split('\n', limit = 2)
        val expires = try {
            if (parts.size != 2) 0L else SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC"); isLenient = false
            }.parse(parts[0])?.time ?: 0L
        } catch (_: java.text.ParseException) { 0L }
        if (expires > System.currentTimeMillis() && parts[1].isNotBlank()) return parts[1]
        clear()
        if (expires > 0L) throw CustomerApiException(401, "Your session has expired. Please sign in again.")
        return null
    }
    override suspend fun clear() = storage.delete("customer-session")
}
