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
        var status = AppAvailability()
        var deletes = 0
        override suspend fun availability() = status
        override suspend fun deleteAccount() { failure?.let { throw it }; deletes++ }
        var pushCalls = 0
        var pushGate: CompletableDeferred<Unit>? = null
        override suspend fun registerPush(token: String) { pushCalls++; pushGate?.await(); failure?.let { throw it } }
        override suspend fun unregisterPush() { pushCalls++; failure?.let { throw it } }
        override suspend fun readNotifications(ids: List<String>) = Unit
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
        repo.failure = CustomerApiException(400, "Invalid code", "OTP_CODE_INVALID")
        model.verify("000000"); runCurrent(); assertEquals(CustomerRoute.Verify, model.state.value.route)
        assertEquals(0, repo.logins); assertEquals("Invalid code", model.state.value.message); assertTrue(model.state.value.codeRejected)
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
    @Test fun `expired session clears financial content and shows session expiry`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.failure = CustomerApiException(401, "Expired"); model.refresh(); runCurrent()
        assertNull(model.state.value.data); assertEquals(1, repo.clears); assertEquals(CustomerRoute.SessionExpired, model.state.value.route)
        model.signInAgain(); assertEquals(CustomerRoute.Phone, model.state.value.route)
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
    @Test fun `service configuration opens maintenance and can recover on retry`() = runTest(dispatcher) {
        val repo = Repository().apply { status = AppAvailability(maintenance = true) }
        val model = model(repo); runCurrent()
        assertEquals(ServiceGate.Maintenance, model.state.value.serviceGate)
        repo.status = AppAvailability(); model.checkServiceStatus(true); runCurrent()
        assertNull(model.state.value.serviceGate)
    }
    @Test fun `recommended version never blocks app use`() = runTest(dispatcher) {
        val repo = Repository().apply { status = AppAvailability() }
        val model = model(repo); runCurrent()
        assertNull(model.state.value.serviceGate)
    }
    @Test fun `server maintenance response is not shown as an offline cache state`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.failure = CustomerApiException(503, "Maintenance", "MAINTENANCE_MODE")
        model.refresh(); runCurrent()
        assertEquals(ServiceGate.Maintenance, model.state.value.serviceGate)
        assertFalse(model.state.value.data?.fromCache ?: true)
    }
    @Test fun `deleting app account signs out and clears all visible account data`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        model.deleteAccount(); runCurrent()
        assertEquals(1, repo.deletes)
        assertEquals(CustomerRoute.Welcome, model.state.value.route)
        assertNull(model.state.value.data)
    }
    @Test fun `failed deletion does not report success or discard account view`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.failure = CustomerNetworkException(java.io.IOException("offline"))
        model.deleteAccount(); runCurrent()
        assertEquals(0, repo.deletes); assertEquals(CustomerRoute.Home, model.state.value.route)
        assertNotNull(model.state.value.message); assertNotNull(model.state.value.data)
    }

    @Test fun `server retry time blocks verification and resending without another request`() = runTest(dispatcher) {
        val repo = Repository(); val model = model(repo); runCurrent(); model.start("3001234567"); runCurrent()
        repo.failure = CustomerApiException(429, "Too many attempts", "OTP_ATTEMPTS_EXCEEDED", 200)
        model.verify("000000"); runCurrent()
        assertEquals(201000L, model.state.value.verifyBlockedUntil)
        repo.failure = null
        model.verify("123456"); model.requestCode(); runCurrent()
        assertEquals(CustomerRoute.Verify, model.state.value.route)
        assertEquals(1, repo.starts); assertEquals(0, repo.logins)
    }
    @Test fun `unregistered number cannot be resubmitted until edited`() = runTest(dispatcher) {
        val repo = Repository().apply { next = "unregistered" }; val model = model(repo); runCurrent()
        model.start("3001234567"); runCurrent(); assertTrue(model.state.value.phoneNotRegistered)
        model.start("3001234567"); runCurrent(); assertEquals(1, repo.starts)
        model.editPhone(); model.start("3001234567"); runCurrent(); assertEquals(2, repo.starts)
    }
    @Test fun `real time change during a request queues exactly one follow up refresh`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.gate = CompletableDeferred(); model.refresh(); runCurrent()
        model.onRealtimeChange(); model.onRealtimeChange(); model.onRealtimeChange()
        repo.gate?.complete(Unit); runCurrent()
        assertEquals(3, repo.reads)
    }

    @Test fun `malformed successful responses retain current data and report a response error`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent(); val before = model.state.value.data
        repo.failure = CustomerResponseException("Missing payments")
        model.refresh(); runCurrent()
        assertEquals(before, model.state.value.data)
        assertEquals("Could not read the server response. Please try again.", model.state.value.message)
        assertFalse(model.state.value.data!!.fromCache)
    }

    @Test fun `unchanged push preferences do not send repeated registrations across binding recreation`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        assertTrue(model.syncPushToken("same-token")); assertTrue(model.syncPushToken("same-token"))
        assertEquals(1, repo.pushCalls)
        assertTrue(model.syncPushToken(null)); assertTrue(model.syncPushToken(null)); assertEquals(2, repo.pushCalls)
    }
    @Test fun `concurrent token synchronization performs one network write`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.pushGate = CompletableDeferred()
        val first = async { model.syncPushToken("same-token") }; val second = async { model.syncPushToken("same-token") }
        runCurrent(); assertEquals(1, repo.pushCalls)
        repo.pushGate!!.complete(Unit); runCurrent()
        assertTrue(first.await()); assertTrue(second.await()); assertEquals(1, repo.pushCalls)
    }
    @Test fun `failed registration remains retryable rather than being cached as success`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.failure = java.io.IOException("offline"); assertFalse(model.syncPushToken("token"))
        repo.failure = null; assertTrue(model.syncPushToken("token")); assertEquals(2, repo.pushCalls)
        assertNull(model.state.value.notificationStatus)
    }

    @Test fun `a late registration response from the previous login cannot suppress the next login registration`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.pushGate = CompletableDeferred()
        val old = async { model.syncPushToken("same-installation") }; runCurrent()
        model.logout(); runCurrent(); repo.next = "password"; model.start("3001234567"); runCurrent(); model.login("password123", true); runCurrent()
        repo.pushGate!!.complete(Unit); runCurrent(); assertFalse(old.await())
        assertTrue(model.syncPushToken("same-installation")); assertEquals(2, repo.pushCalls)
    }

    @Test fun `a previous session registration failure cannot add an error to the next account`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.pushGate = CompletableDeferred()
        val old = async { model.syncPushToken("installation") }; runCurrent()
        model.logout(); runCurrent(); repo.next = "password"; model.start("3001234567"); runCurrent(); model.login("password123", true); runCurrent()
        repo.failure = java.io.IOException("late response failure")
        repo.pushGate!!.complete(Unit); runCurrent(); assertFalse(old.await())
        assertNull(model.state.value.notificationStatus)
    }
    @Test fun `cancellation releases push synchronization and does not cache an incomplete write`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        repo.pushGate = CompletableDeferred()
        val cancelled = launch { model.syncPushToken("installation") }; runCurrent(); cancelled.cancelAndJoin()
        repo.pushGate = null
        assertTrue(model.syncPushToken("installation")); assertEquals(2, repo.pushCalls)
    }

}
