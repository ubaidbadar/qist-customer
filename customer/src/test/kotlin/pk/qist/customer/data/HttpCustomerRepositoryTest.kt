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
        for (body in listOf("Unauthorized", """{"error":{"message":[]}}""")) {
            val repository = HttpCustomerRepository(MemorySession().apply { token = "stored" }, "https://example.com/", client { 401 to body })
            repository.restore()
            try { repository.dashboard(); fail("Expected unauthorized") }
            catch (error: CustomerApiException) { assertEquals(401, error.status) }
        }
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
    @Test fun `phone routing and OTP proof do not persist a login session`() = runBlocking {
        val session = MemorySession()
        val paths = mutableListOf<String>()
        val repository = HttpCustomerRepository(session, "https://example.com/api/v1/customer-app/", client { request ->
            paths.add(request.url.encodedPath)
            200 to when {
                request.url.encodedPath.endsWith("/start") -> """{"data":{"next":"otp","retryAfterSeconds":60}}"""
                else -> """{"data":{"setupToken":"proof","expiresAt":"2026-12-07T00:00:00.000Z"}}"""
            }
        })
        assertEquals("otp", repository.start("03001234567").next)
        assertEquals("proof", repository.verifyCode("03001234567", "123456"))
        assertNull(session.token)
        assertEquals(listOf("/api/v1/customer-app/auth/start", "/api/v1/customer-app/auth/verify-code"), paths)
    }
    @Test fun `password setup consumes proof endpoint and stores returned session`() = runBlocking {
        val session = MemorySession()
        val repository = HttpCustomerRepository(session, "https://example.com/", client { request ->
            assertEquals("/auth/set-password", request.url.encodedPath)
            val buffer = okio.Buffer(); request.body?.writeTo(buffer)
            val body = buffer.readUtf8(); assertTrue(body.contains("setupToken")); assertTrue(body.contains("proof"))
            200 to """{"data":{"accessToken":"new-session","expiresAt":"2026-12-07T00:00:00.000Z"}}"""
        })
        repository.setPassword("03001234567", "proof", "password123")
        assertEquals("new-session", session.token)
    }

    @Test fun `transport failure restores saved plan but authorization failure never does`() = runBlocking {
        val storage = MemoryTextStore()
        val cache = BoundOverviewCache(storage)
        val session = MemorySession().apply { token = "stored" }
        var mode = 200
        val repository = HttpCustomerRepository(session, "https://example.com/", client {
            if (mode == -1) throw java.io.IOException("offline")
            mode to if (mode == 200) """{"data":${overviewFixture()}}""" else """{"error":{"code":"UNAUTHORIZED","message":"Expired"}}"""
        }, overviewCache = cache)
        repository.restore(); repository.dashboard()
        mode = -1
        assertTrue(repository.dashboard().fromCache)
        mode = 401
        try { repository.dashboard(); fail("Expected unauthorized") } catch (error: CustomerApiException) { assertEquals(401, error.status) }
    }
    @Test fun `account deletion uses DELETE and clears token and saved snapshot only after success`() = runBlocking {
        val storage = MemoryTextStore(); val cache = BoundOverviewCache(storage)
        cache.save("stored", overviewFixture(), 1)
        val session = MemorySession().apply { token = "stored" }
        val repository = HttpCustomerRepository(session, "https://example.com/", client { request ->
            assertEquals("DELETE", request.method); assertEquals("/account", request.url.encodedPath)
            assertEquals("Bearer stored", request.header("Authorization"))
            200 to """{"data":{"ok":true}}"""
        }, overviewCache = cache)
        repository.restore(); repository.deleteAccount()
        assertNull(session.token); assertTrue(storage.values.isEmpty())
    }
    @Test fun `service config and app version header use the public customer endpoint`() = runBlocking {
        val repository = HttpCustomerRepository(MemorySession(), "https://example.com/", client { request ->
            assertEquals("/config", request.url.encodedPath)
            assertEquals("217111869", request.header("X-Customer-App-Version"))
            assertNull(request.header("Authorization"))
            200 to """{"data":{"maintenance":true,"minimumVersionCode":100}}"""
        }, appVersionCode = 217111869)
        assertTrue(repository.availability().maintenance)
    }

    @Test fun `expired session cannot read an offline snapshot`() = runBlocking {
        val storage = MemoryTextStore(); val cache = BoundOverviewCache(storage)
        val session = MemorySession().apply { token = "stored" }
        cache.save("stored", overviewFixture(), 1)
        val repository = HttpCustomerRepository(session, "https://example.com/", client { throw java.io.IOException("offline") }, overviewCache = cache)
        repository.restore(); session.token = null
        try { repository.dashboard(); fail("Expected expired session") }
        catch (error: CustomerApiException) { assertEquals(401, error.status) }
        assertTrue(storage.values.isEmpty())
    }
    @Test fun `maintenance error never falls back to saved data or deletes it`() = runBlocking {
        val storage = MemoryTextStore(); val cache = BoundOverviewCache(storage)
        val session = MemorySession().apply { token = "stored" }
        cache.save("stored", overviewFixture(), 1)
        val repository = HttpCustomerRepository(session, "https://example.com/", client {
            503 to """{"error":{"code":"MAINTENANCE_MODE","message":"Maintenance"}}"""
        }, overviewCache = cache)
        repository.restore()
        try { repository.dashboard(); fail("Expected maintenance") }
        catch (error: CustomerApiException) { assertEquals("MAINTENANCE_MODE", error.code) }
        assertNotNull(cache.read("stored", "plan-a"))
    }

}
