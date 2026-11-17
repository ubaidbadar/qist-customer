package pk.qist.customer.documents

import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import pk.qist.customer.data.CustomerApiException
import pk.qist.customer.data.SessionStore

class DocumentApiTest {
    private class MemorySession(var token: String? = "session-a") : SessionStore {
        override suspend fun read() = token
        override suspend fun save(token: String, expiresAt: String) { this.token = token }
        override suspend fun clear() { token = null }
    }
    private fun client(handler: (Request) -> Pair<Int, String>) = OkHttpClient.Builder().addInterceptor { chain ->
        val (code, body) = handler(chain.request())
        Response.Builder().request(chain.request()).code(code).message("Test").protocol(Protocol.HTTP_1_1)
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()
    @Test fun `request uses current bearer and selected plan in one metadata call`() = runBlocking {
        val requests = mutableListOf<Request>()
        val api = DocumentApi(MemorySession(), "https://example.invalid/api/v1/customer-app/", client = client {
            requests.add(it)
            200 to """{"data":{"items":[],"requests":[],"nextCursor":null,"canUpload":true,"policy":{"maxFileBytes":10485760,"retentionNotice":"Fixture"}}}"""
        })
        assertTrue(api.list("plan-a").canUpload)
        assertEquals(1, requests.size)
        assertEquals("https://example.invalid/api/v1/customer-app/documents?contractId=plan-a", requests.single().url.toString())
        assertEquals("Bearer session-a", requests.single().header("Authorization"))
    }
    @Test fun `queued session binding rejects changed account before any network request`() = runBlocking {
        var calls = 0
        val sessions = MemorySession()
        val api = DocumentApi(sessions, "https://example.invalid/", expectedScope = sessionScope("session-a"), client = client { calls++; 500 to "" })
        sessions.token = "session-b"
        try { api.list("plan-a"); fail("Expected session binding rejection") }
        catch (error: CustomerApiException) { assertEquals(401, error.status) }
        assertEquals(0, calls)
    }
    @Test fun `server errors do not expose provider payload and preserve revocation status`() = runBlocking {
        val api = DocumentApi(MemorySession(), "https://example.invalid/", client = client { 401 to "private provider detail" })
        try { api.list("plan-a"); fail("Expected failure") }
        catch (error: CustomerApiException) { assertEquals(401, error.status); assertFalse(error.message.orEmpty().contains("private provider")) }
    }
}
