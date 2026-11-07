package pk.qist.customer.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import pk.qist.customer.data.*

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }
    private class Repository : CustomerRepository {
        var restored = false
        var next = "otp"
        var reads = 0
        var logins = 0
        var starts = 0
        var clears = 0
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun restore() = restored
        override suspend fun start(phone: String): AuthStart { starts++; assertEquals("03001234567", phone); gate?.await(); failure?.let { throw it }; return AuthStart(next, 60) }
        override suspend fun requestCode(phone: String): Int { failure?.let { throw it }; return 60 }
        override suspend fun verifyCode(phone: String, code: String): String { failure?.let { throw it }; return "proof" }
        override suspend fun setPassword(phone: String, setupToken: String, password: String) { assertEquals("proof", setupToken); failure?.let { throw it }; logins++ }
        override suspend fun resetPassword(phone: String, code: String, password: String) = Unit
        override suspend fun login(phone: String, password: String) { failure?.let { throw it }; logins++ }
        override suspend fun dashboard(contractId: String?): DashboardSample { reads++; gate?.await(); failure?.let { throw it }; return parseOverview(overviewFixture()) }
        override suspend fun payments(contractId: String, cursor: String): PaymentPage { failure?.let { throw it }; return PaymentPage(parseOverview(overviewFixture()).payments, null) }
        override suspend fun logout() = Unit
        override suspend fun clearSession() { clears++ }
    }
    private fun model(repo: Repository, accepted: Boolean = true) = CustomerViewModel(repo, accepted) { 1000L }
    @Test fun `fresh session shows welcome without plan requests`() = runTest(dispatcher) {
        val repo = Repository(); val model = model(repo); runCurrent()
        assertEquals(CustomerRoute.Welcome, model.state.value.route); assertEquals(0, repo.reads)
    }
    @Test fun `phone with plan and no password goes to OTP then verified password setup`() = runTest(dispatcher) {
        val repo = Repository(); val model = model(repo); runCurrent()
        model.start("3001234567"); runCurrent()
        assertEquals(CustomerRoute.Verify, model.state.value.route); assertEquals(61000L, model.state.value.deadline)
        model.verify("123456"); runCurrent(); assertEquals(CustomerRoute.SetPassword, model.state.value.route)
        model.savePassword("password123", true); runCurrent()
        assertEquals(CustomerRoute.Home, model.state.value.route); assertEquals(1, repo.logins); assertEquals(1, repo.reads)
    }
    @Test fun `existing password bypasses OTP and loads only one overview`() = runTest(dispatcher) {
        val repo = Repository().apply { next = "password" }; val model = model(repo); runCurrent()
        model.start("3001234567"); runCurrent(); assertEquals(CustomerRoute.Password, model.state.value.route)
        model.login("password123", true); runCurrent()
        assertEquals(1, repo.reads); assertEquals(CustomerRoute.Home, model.state.value.route)
    }
    @Test fun `no plan stays on phone screen with retailer guidance`() = runTest(dispatcher) {
        val repo = Repository().apply { next = "unregistered" }; val model = model(repo); runCurrent(); model.begin()
        model.start("3001234567"); runCurrent()
        assertEquals(CustomerRoute.Phone, model.state.value.route); assertNotNull(model.state.value.message); assertEquals(0, repo.reads)
    }
    @Test fun `wrong OTP stays on verification and does not create session`() = runTest(dispatcher) {
        val repo = Repository(); val model = model(repo); runCurrent(); model.start("3001234567"); runCurrent()
        repo.failure = CustomerApiException(400, "Invalid code")
        model.verify("000000"); runCurrent(); assertEquals(CustomerRoute.Verify, model.state.value.route)
        assertEquals(0, repo.logins); assertEquals("Invalid code", model.state.value.message)
    }
    @Test fun `duplicate phone submits do not send duplicate requests`() = runTest(dispatcher) {
        val repo = Repository(); val model = model(repo); runCurrent(); repo.gate = CompletableDeferred()
        model.start("3001234567"); model.start("3001234567"); runCurrent(); assertEquals(1, repo.starts)
        repo.gate?.complete(Unit); runCurrent()
    }
    @Test fun `restore keeps consent boundary before plan data`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo, false); runCurrent()
        assertEquals(CustomerRoute.Consent, model.state.value.route); assertEquals(0, repo.reads)
        model.acceptPrivacy(); runCurrent(); assertEquals(1, repo.reads)
    }
    @Test fun `offline pull refresh keeps loaded content and clears refresh state`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent(); val old = model.state.value.data
        repo.failure = java.io.IOException("offline"); model.refresh(); runCurrent()
        assertEquals(old, model.state.value.data); assertFalse(model.state.value.refreshing); assertNotNull(model.state.value.message)
    }
    @Test fun `expired session clears financial content and returns to phone`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.failure = CustomerApiException(401, "Expired"); model.refresh(); runCurrent()
        assertNull(model.state.value.data); assertEquals(1, repo.clears); assertEquals(CustomerRoute.Phone, model.state.value.route)
    }
    @Test fun `pagination deduplicates receipt ids`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent(); model.morePayments(); runCurrent()
        assertEquals(2, model.state.value.data?.payments?.size); assertNull(model.state.value.data?.nextCursor)
    }
    @Test fun `failed resend does not discard valid setup proof`() = runTest(dispatcher) {
        val repo = Repository(); val model = model(repo); runCurrent(); model.start("3001234567"); runCurrent(); model.verify("123456"); runCurrent()
        repo.failure = CustomerApiException(429, "Wait before resending"); model.requestCode(true); runCurrent()
        assertEquals(CustomerRoute.SetPassword, model.state.value.route)
        repo.failure = null; model.savePassword("password123", true); runCurrent(); assertEquals(1, repo.logins)
    }
}
