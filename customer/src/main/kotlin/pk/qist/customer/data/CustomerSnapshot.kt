package pk.qist.customer.data

import java.io.IOException
import java.security.GeneralSecurityException
import java.security.MessageDigest
import kotlinx.serialization.json.*

/** Implementations must keep all text encrypted and outside Android backup. */
internal interface SecureTextStore {
    suspend fun read(name: String): String?
    suspend fun write(name: String, value: String)
    suspend fun delete(name: String)
}
internal data class SavedOverview(val data: JsonObject, val savedAt: Long)
internal interface OverviewCache {
    suspend fun read(token: String, contractId: String?): SavedOverview?
    suspend fun save(token: String, data: JsonObject, savedAt: Long): Boolean
    suspend fun clear()
}

/** One last-viewed snapshot, bound to the exact session; never fetch other phones to fill the cache. */
internal class BoundOverviewCache(private val storage: SecureTextStore) : OverviewCache {
    private val name = "customer-overview"
    private fun owner(token: String) = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    override suspend fun read(token: String, contractId: String?): SavedOverview? {
        val text = storage.read(name) ?: return null
        return try {
            val payload = Json.parseToJsonElement(text).jsonObject
            if (payload.getValue("owner").jsonPrimitive.content != owner(token)) { clear(); return null }
            val data = payload.getValue("overview").jsonObject
            val id = (data["contract"] as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull
            if (contractId != null && id != contractId) return null
            SavedOverview(data, payload.getValue("savedAt").jsonPrimitive.long)
        } catch (_: IllegalArgumentException) { clear(); null }
        catch (_: NoSuchElementException) { clear(); null }
    }
    override suspend fun save(token: String, data: JsonObject, savedAt: Long): Boolean {
        val payload = buildJsonObject { put("owner", owner(token)); put("savedAt", savedAt); put("overview", data) }
        return try { storage.write(name, payload.toString()); true }
        catch (_: IOException) { false }
        catch (_: GeneralSecurityException) { false }
    }
    override suspend fun clear() = storage.delete(name)
}
