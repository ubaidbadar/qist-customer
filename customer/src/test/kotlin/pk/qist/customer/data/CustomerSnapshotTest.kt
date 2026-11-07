package pk.qist.customer.data

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

internal class MemoryTextStore : SecureTextStore {
    val values = mutableMapOf<String, String>()
    var failWrite = false
    override suspend fun read(name: String) = values[name]
    override suspend fun write(name: String, value: String) { if (failWrite) throw IOException("Storage full"); values[name] = value }
    override suspend fun delete(name: String) { values.remove(name) }
}
class CustomerSnapshotTest {
    @Test fun `offline snapshot survives repository recreation and is bound to its session`() = runBlocking {
        val storage = MemoryTextStore()
        assertTrue(BoundOverviewCache(storage).save("token-a", overviewFixture(), 1234))
        val restored = BoundOverviewCache(storage).read("token-a", "plan-a")
        assertEquals(1234L, restored?.savedAt); assertNotNull(restored?.data)
        assertNull(BoundOverviewCache(storage).read("token-b", "plan-a"))
        assertTrue(storage.values.isEmpty())
    }
    @Test fun `wrong phone selection never receives another phone saved data`() = runBlocking {
        val cache = BoundOverviewCache(MemoryTextStore())
        cache.save("token-a", overviewFixture(), 1234)
        assertNull(cache.read("token-a", "plan-b"))
        assertNotNull(cache.read("token-a", "plan-a"))
    }
    @Test fun `corrupt snapshot is cleared and cannot fabricate a dashboard`() = runBlocking {
        val storage = MemoryTextStore().apply { values["customer-overview"] = "bad-json" }
        assertNull(BoundOverviewCache(storage).read("token-a", null)); assertTrue(storage.values.isEmpty())
    }
    @Test fun `cache reports storage failure without hiding successful online data`() = runBlocking {
        val storage = MemoryTextStore().apply { failWrite = true }
        assertFalse(BoundOverviewCache(storage).save("token-a", overviewFixture(), 1))
    }
    @Test fun `session expiry removes unusable sign in data`() = runBlocking {
        val storage = MemoryTextStore(); val session = CustomerSessionStore(storage)
        session.save("expired", "2000-01-01T00:00:00.000Z")
        try { session.read(); fail("Expected expired session") } catch (error: CustomerApiException) { assertEquals(401, error.status) }
        assertNull(storage.values["customer-session"])
        session.save("valid", "2099-01-01T00:00:00.000Z")
        assertEquals("valid", session.read())
    }
    @Test fun `malformed session does not crash or authenticate`() = runBlocking {
        val storage = MemoryTextStore().apply { values["customer-session"] = "broken" }
        assertNull(CustomerSessionStore(storage).read())
    }
    @Test fun `maintenance blocks but optional version recommendations do not`() {
        assertEquals(ServiceGate.Maintenance, AppAvailability(true).gate())
        assertNull(AppAvailability(false).gate())
        assertNull(AppAvailability(false).gate())
    }
}
