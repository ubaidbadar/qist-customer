package pk.qist.device.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HttpDeviceApiTest {
    private val server = MockWebServer()
    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: HttpDeviceApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
        api = HttpDeviceApi(client, server.url("/api/v1"), Json { ignoreUnknownKeys = true }, dispatcher)
    }

    @After
    fun tearDown() = server.close()

    private fun reply(code: Int, body: String = "") =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun `enrol posts the token without auth and returns the credential`() = runTest(dispatcher) {
        reply(201, """{"data":{"deviceId":"d1","token":"d1.secret","checkInSeconds":900}}""")

        val result = api.enroll(EnrollRequest(token = "qr-token", appVersion = "1", androidVersion = "14"))

        assertEquals(ApiResult.Success(DeviceCredential("d1", "d1.secret", 900)), result)
        val request = server.takeRequest()
        assertEquals("/api/v1/device/enroll", request.url.encodedPath)
        assertEquals(null, request.headers["Authorization"])
    }

    @Test
    fun `check-in sends the credential as a bearer token and reads commands`() = runTest(dispatcher) {
        reply(200, CHECK_IN_BODY)

        val result = api.checkIn("d1.secret", CheckInRequest(appVersion = "1", lockState = LockState.UNLOCKED))

        val response = (result as ApiResult.Success).value
        assertEquals(LockState.LOCKED, response.lockState)
        assertEquals(listOf(CommandDto("c1", "lock")), response.commands)
        assertEquals(150_000L, response.lockScreen.amountDue)
        assertEquals("Bearer d1.secret", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `a 401 becomes Unauthorized`() = runTest(dispatcher) {
        reply(401, """{"error":{"code":"unauthorized","message":"no"}}""")
        assertEquals(ApiResult.Unauthorized, api.checkIn("bad", CheckInRequest("1", LockState.UNLOCKED)))
    }

    @Test
    fun `other error statuses become ServerError with the status`() = runTest(dispatcher) {
        reply(409)
        assertEquals(ApiResult.ServerError(409), api.acknowledge("t", "c1", AckRequest(AckRequest.Outcome.APPLIED)))
    }

    @Test
    fun `an unreadable body becomes ServerError instead of crashing`() = runTest(dispatcher) {
        reply(200, "<html>proxy login</html>")
        assertEquals(ApiResult.ServerError(200), api.checkIn("t", CheckInRequest("1", LockState.UNLOCKED)))
    }

    @Test
    fun `no server becomes NetworkError`() = runTest(dispatcher) {
        server.close()
        assertEquals(ApiResult.NetworkError, api.checkIn("t", CheckInRequest("1", LockState.UNLOCKED)))
    }

    @Test
    fun `acknowledge posts the result to the command's url`() = runTest(dispatcher) {
        reply(200, """{"data":{"_id":"c1","status":"acknowledged"}}""")

        val result = api.acknowledge("t", "c1", AckRequest(AckRequest.Outcome.FAILED, "not device owner"))

        assertEquals(ApiResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("/api/v1/device/commands/c1/ack", request.url.encodedPath)
        assertEquals("""{"result":"failed","detail":"not device owner"}""", request.body?.utf8())
    }

    private companion object {
        val CHECK_IN_BODY = """
            {"data":{"lockState":"locked","checkInSeconds":900,
            "commands":[{"_id":"c1","type":"lock"}],
            "lockScreen":{"locked":true,"customerName":"Ali","deviceLabel":"Oppo A18","amountDue":150000,
            "nextDueDate":"2026-10-12","branchName":"Saddar","branchPhone":"0300 1234567","message":"Please pay"}}}
        """.trimIndent()
    }
}
