package pk.qist.customer.data

import java.text.SimpleDateFormat
import java.text.ParsePosition
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
        val expires = if (parts.size != 2) 0L else {
            val position = ParsePosition(0)
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC"); isLenient = false
            }.parse(parts[0], position)
            // DateFormat otherwise accepts a valid prefix followed by corrupt trailing text.
            date?.takeIf { position.index == parts[0].length }?.time ?: 0L
        }
        if (expires > System.currentTimeMillis() && parts[1].isNotBlank()) return parts[1]
        clear()
        if (expires > 0L) throw CustomerApiException(401, "Your session has expired. Please sign in again.")
        return null
    }
    override suspend fun clear() = storage.delete("customer-session")
}
