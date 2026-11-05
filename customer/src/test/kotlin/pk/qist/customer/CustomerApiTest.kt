package pk.qist.customer

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pk.qist.customer.data.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomerApiTest {
    private val server = MockWebServer()
    private lateinit var api: HttpCustomerApi
    @Before fun setup() {
        server.start()
        api = HttpCustomerApi(OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build(), server.url("/api/v1/customer-app/").toString())
    }
    @After fun close() { server.close() }
    private fun reply(code: Int, body: String) { server.enqueue(MockResponse.Builder().code(code).body(body).build()) }
    @Test fun loginSendsPhoneAndPasswordAndNoStaffAuthorization() = runTest {
        reply(200, """{"data":{"accessToken":"customer-token","expiresAt":"2026-12-05"}}""")
        assertEquals("customer-token", api.login("03001234567", "my-password").accessToken)
        val request = server.takeRequest()
        assertEquals("/api/v1/customer-app/auth/login", request.url.encodedPath)
        assertNull(request.headers["Authorization"])
        assertTrue(request.body?.utf8()?.contains("03001234567") == true)
    }
    @Test fun emptyOverviewIsParsedOnceWithoutInventedRecords() = runTest {
        reply(200, """{"data":{"profile":{"name":"Customer","phone":"03001234567"},"contracts":[],"contract":null,"payments":{"items":[],"nextCursor":null},"today":"2026-11-05"}}""")
        val result = api.overview("customer-token")
        assertNull(result.plan); assertTrue(result.payments.items.isEmpty()); assertEquals(1, server.requestCount)
        assertEquals("Bearer customer-token", server.takeRequest().headers["Authorization"])
    }
    @Test fun failuresAndMalformedResponsesNeverBecomeFakeSuccess() = runTest {
        reply(401, """{"error":{"code":"UNAUTHORIZED","message":"Expired"}}""")
        try { api.overview("old-token"); fail("Expected unauthorized") } catch (error: CustomerApiException) { assertEquals(401, error.status) }
        reply(200, "not JSON")
        try { api.login("03001234567", "password"); fail("Expected unreadable response") } catch (_: IOException) { }
        reply(503, """{"error":{"code":"SERVICE_UNAVAILABLE","message":"WhatsApp verification is unavailable"}}""")
        try { api.requestPassword("03001234567"); fail("Expected unavailable") } catch (error: CustomerApiException) { assertEquals(503, error.status) }
    }
}
