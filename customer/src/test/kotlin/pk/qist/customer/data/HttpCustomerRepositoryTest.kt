package pk.qist.customer.data

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class HttpCustomerRepositoryTest {
    private class MemorySession : SessionStore {
        var token: String? = null
        override suspend fun read() = token
        override suspend fun save(token: String, expiresAt: String) { this.token = token }
        override suspend fun clear() { token = null }
    }
    private fun client(handler: (Request) -> Pair<Int, String>): OkHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val (code, body) = handler(chain.request())
        Response.Builder().request(chain.request()).code(code).message("Test").protocol(Protocol.HTTP_1_1)
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()

    @Test fun `login persists token and overview uses bearer and exact URL`() = runBlocking {
        val requests = mutableListOf<Request>()
        val session = MemorySession()
        val repository = HttpCustomerRepository(session, "https://example.com/api/v1/customer-app", client { request ->
            requests.add(request)
            200 to if (request.url.encodedPath.endsWith("/login")) """{"data":{"accessToken":"token","expiresAt":"2026-12-07T00:00:00.000Z"}}""" else """{"data":${overviewFixture()}}"""
        })
        repository.login("03001234567", "password123")
        repository.dashboard("plan-a")
        assertEquals("token", session.token)
        assertEquals("https://example.com/api/v1/customer-app/overview?contractId=plan-a", requests.last().url.toString())
        assertEquals("Bearer token", requests.last().header("Authorization"))
        assertEquals(2, requests.size)
    }
    @Test fun `server error remains visible and code response never logs in`() = runBlocking {
        val session = MemorySession()
        val repository = HttpCustomerRepository(session, "https://example.com/", client { 503 to """{"error":{"message":"WhatsApp is not configured"}}""" })
        try { repository.requestCode("03001234567"); fail("Expected failure") }
        catch (error: CustomerApiException) { assertEquals(503, error.status); assertEquals("WhatsApp is not configured", error.message) }
        assertNull(session.token)
    }
    @Test fun `logout revokes session before clearing local token`() = runBlocking {
        val session = MemorySession().apply { token = "stored" }
        val repository = HttpCustomerRepository(session, "https://example.com/", client { request ->
            assertEquals("POST", request.method)
            assertEquals("Bearer stored", request.header("Authorization"))
            200 to """{"data":{"ok":true}}"""
        })
        assertTrue(repository.restore())
        repository.logout()
        assertNull(session.token)
    }
    @Test fun `unauthorized HTML response preserves status`() = runBlocking {
        val repository = HttpCustomerRepository(MemorySession().apply { token = "stored" }, "https://example.com/", client { 401 to "Unauthorized" })
        repository.restore()
        try { repository.dashboard(); fail("Expected unauthorized") }
        catch (error: CustomerApiException) { assertEquals(401, error.status) }
    }
    @Test fun `pagination sends contract and cursor`() = runBlocking {
        val repository = HttpCustomerRepository(MemorySession().apply { token = "stored" }, "https://example.com/api/v1/customer-app/", client { request ->
            assertEquals("plan-a", request.url.queryParameter("contractId"))
            assertEquals("cursor-a", request.url.queryParameter("cursor"))
            200 to """{"data":{"items":[],"nextCursor":null}}"""
        })
        repository.restore()
        assertTrue(repository.payments("plan-a", "cursor-a").items.isEmpty())
    }
}
