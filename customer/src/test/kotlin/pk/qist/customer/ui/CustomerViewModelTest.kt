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
        var localRequests = 0
        var remoteRequests = 0
        var claims = 0
        var exchanges = 0
        var cancellations = 0
        var remoteState = DeviceClaim("pending")
        var deviceIdentity = DeviceIdentity("password", "03001234567", "proof")
        var lastNonce = ""
        override suspend fun localChallenge(nonce: String): DeviceChallenge { localRequests++; lastNonce = nonce; return DeviceChallenge("a".repeat(24), "2099-01-01T00:00:00.000Z") }
        override suspend fun remoteChallenge(phone: String, nonce: String): DeviceChallenge { remoteRequests++; lastNonce = nonce; return DeviceChallenge("b".repeat(24), "2099-01-01T00:00:00.000Z", "123") }
        override suspend fun claimDevice(requestId: String, nonce: String): DeviceClaim { claims++; return if (requestId.startsWith("a")) DeviceClaim("approved", "t".repeat(43)) else remoteState }
        override suspend fun exchangeDevice(ticket: String, nonce: String): DeviceIdentity { exchanges++; failure?.let { throw it }; return deviceIdentity }
        override suspend fun cancelDevice(requestId: String, nonce: String) { cancellations++ }
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
        val requestedPlans = mutableListOf<String?>()
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
        override suspend fun dashboard(contractId: String?): DashboardSample { reads++; requestedPlans.add(contractId); gate?.await(); failure?.let { throw it }; return parseOverview(overviewFixture()).let { it.copy(contractId = contractId ?: it.id) } }
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

    @Test fun `initial account loading uses skeleton state without pull indicator`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = Repository().apply { restored = true; this.gate = gate }
        val model = model(repo); runCurrent()
        assertTrue(model.state.value.busy); assertNull(model.state.value.data); assertFalse(model.state.value.refreshing)
        gate.complete(Unit); runCurrent()
        assertNotNull(model.state.value.data); assertFalse(model.state.value.busy)
    }
    @Test fun `foreground update keeps content visible and never starts pull indicator`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val old = model.state.value.data; val gate = CompletableDeferred<Unit>(); repo.gate = gate
        model.onForeground(); runCurrent()
        assertFalse(model.state.value.busy); assertEquals(old, model.state.value.data); assertFalse(model.state.value.refreshing)
        model.onForeground(); model.onForeground(); runCurrent(); assertEquals(2, repo.reads)
        gate.complete(Unit); runCurrent(); assertFalse(model.state.value.busy); assertFalse(model.state.value.refreshing)
    }
    @Test fun `notification update keeps content visible without pull indicator`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val old = model.state.value.data; val gate = CompletableDeferred<Unit>(); repo.gate = gate
        model.onRealtimeChange(); runCurrent()
        assertFalse(model.state.value.busy); assertEquals(old, model.state.value.data); assertFalse(model.state.value.refreshing)
        gate.complete(Unit); runCurrent(); assertEquals(2, repo.reads); assertFalse(model.state.value.refreshing)
    }
    @Test fun `pull indicator ends before the coalesced notification follow up`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val manual = CompletableDeferred<Unit>(); repo.gate = manual
        model.refresh(); runCurrent(); assertTrue(model.state.value.refreshing)
        repeat(3) { model.onRealtimeChange() }
        val followUp = CompletableDeferred<Unit>(); repo.gate = followUp
        manual.complete(Unit); runCurrent()
        assertEquals(3, repo.reads); assertFalse(model.state.value.busy); assertFalse(model.state.value.refreshing)
        followUp.complete(Unit); runCurrent(); assertFalse(model.state.value.busy)
    }
    @Test fun `silent update failure keeps loaded data and never shows pull indicator`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val old = model.state.value.data
        repo.failure = java.io.IOException("offline"); model.onForeground(); runCurrent()
        assertEquals(old, model.state.value.data); assertNotNull(model.state.value.message)
        assertFalse(model.state.value.refreshing); assertFalse(model.state.value.busy)
    }
    @Test fun `foreground and notification events cannot load a signed out account`() = runTest(dispatcher) {
        val repo = Repository(); val model = model(repo); runCurrent()
        model.onForeground(); model.onRealtimeChange(); model.refresh(); runCurrent()
        assertEquals(0, repo.reads); assertFalse(model.state.value.refreshing)
    }

    @Test fun `logout interrupts a silent refresh and cannot be undone by its late completion`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val refresh = CompletableDeferred<Unit>(); repo.gate = refresh
        model.onForeground(); runCurrent()
        model.logout(); runCurrent()
        assertEquals(CustomerRoute.Welcome, model.state.value.route)
        refresh.complete(Unit); runCurrent()
        assertNull(model.state.value.data); assertFalse(model.state.value.busy)
    }

    @Test fun `switching phone takes priority over a silent refresh`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val background = CompletableDeferred<Unit>(); repo.gate = background
        model.onForeground(); runCurrent()
        val switching = CompletableDeferred<Unit>(); repo.gate = switching
        model.selectPlan("plan-b"); runCurrent()
        assertEquals(listOf(null, "plan-a", "plan-b"), repo.requestedPlans)
        assertTrue(model.state.value.switchingPlan); assertTrue(model.state.value.busy)
        background.complete(Unit); runCurrent()
        assertTrue(model.state.value.busy)
        switching.complete(Unit); runCurrent()
        assertEquals("plan-b", model.state.value.data?.id); assertFalse(model.state.value.busy)
    }

    @Test fun `manual refresh interrupts a silent refresh and owns the top indicator`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val background = CompletableDeferred<Unit>(); repo.gate = background
        model.onForeground(); runCurrent()
        val manual = CompletableDeferred<Unit>(); repo.gate = manual
        model.refresh(); runCurrent()
        assertTrue(model.state.value.refreshing); assertEquals(3, repo.reads)
        background.complete(Unit); runCurrent(); assertTrue(model.state.value.refreshing)
        manual.complete(Unit); runCurrent(); assertFalse(model.state.value.refreshing)
    }

    @Test fun `notification burst during a silent refresh queues a single silent follow up`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val background = CompletableDeferred<Unit>(); repo.gate = background
        model.onForeground(); runCurrent()
        repeat(1000) { model.onRealtimeChange() }
        val followUp = CompletableDeferred<Unit>(); repo.gate = followUp
        background.complete(Unit); runCurrent()
        assertEquals(3, repo.reads); assertFalse(model.state.value.refreshing)
        followUp.complete(Unit); runCurrent(); assertEquals(3, repo.reads)
    }

    @Test fun `silent authentication failure clears account and does not run queued updates`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }; val model = model(repo); runCurrent()
        val background = CompletableDeferred<Unit>(); repo.gate = background
        model.onForeground(); runCurrent(); model.onRealtimeChange()
        repo.failure = CustomerApiException(401, "Expired")
        background.complete(Unit); runCurrent()
        assertEquals(CustomerRoute.SessionExpired, model.state.value.route)
        assertNull(model.state.value.data); assertEquals(2, repo.reads)
        assertFalse(model.state.value.busy); assertFalse(model.state.value.refreshing)
    }

    @Test fun `offline login does not claim to show saved account data`() = runTest(dispatcher) {
        val repo = Repository().apply { next = "password" }; val model = model(repo); runCurrent()
        model.start("3001234567"); runCurrent()
        repo.failure = CustomerNetworkException(java.io.IOException("offline"))
        model.login("password123", true); runCurrent()
        assertEquals(CustomerRoute.Password, model.state.value.route); assertNull(model.state.value.data)
        assertEquals("Could not connect. Check your internet and try again.", model.state.value.message)
    }

    @Test fun `immediate cancellation cannot start an obsolete queued refresh over a user action`() = runTest(dispatcher) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repo = Repository().apply { restored = true }; val model = model(repo)
        val background = CompletableDeferred<Unit>(); repo.gate = background
        model.onForeground(); model.onRealtimeChange()
        val switching = CompletableDeferred<Unit>(); repo.gate = switching
        model.selectPlan("plan-b")
        assertEquals(listOf(null, "plan-a", "plan-b"), repo.requestedPlans)
        assertTrue(model.state.value.busy); assertTrue(model.state.value.switchingPlan)
        model.onForeground(); assertEquals(3, repo.reads)
        switching.complete(Unit); runCurrent()
        assertEquals("plan-b", model.state.value.data?.id)
        assertEquals("plan-b", repo.requestedPlans.last())
        assertEquals(4, repo.reads); assertFalse(model.state.value.busy)
    }

    private class Locker(private val available: Boolean = true, private val approved: Boolean = true) : LockerAuthenticator {
        var calls = 0
        override suspend fun available() = available
        override suspend fun authenticate(requestId: String, nonce: String): Boolean { calls++; return approved }
    }

    @Test fun `trusted same phone goes directly to first password setup without sending a code`() = runTest(dispatcher) {
        val repo = Repository(); val locker = Locker()
        val model = CustomerViewModel(repo, true, locker = locker, clock = { testScheduler.currentTime })
        runCurrent()
        assertEquals(CustomerRoute.SetPassword, model.state.value.route)
        assertEquals("3001234567", model.state.value.phone)
        assertEquals(0, repo.starts); assertEquals(0, repo.logins); assertEquals(1, repo.exchanges)
        assertTrue(repo.lastNonce.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertFalse(model.state.value.toString().contains(repo.lastNonce))
        model.savePassword("password123", true); runCurrent()
        assertEquals(CustomerRoute.Home, model.state.value.route)
    }
    @Test fun `existing customer on trusted phone signs in automatically and loads one overview`() = runTest(dispatcher) {
        val repo = Repository().apply { deviceIdentity = DeviceIdentity("session") }
        val model = CustomerViewModel(repo, true, locker = Locker(), clock = { testScheduler.currentTime })
        runCurrent()
        assertEquals(CustomerRoute.Home, model.state.value.route); assertEquals(1, repo.reads)
        assertEquals(0, repo.starts)
    }
    @Test fun `missing or rejecting Locker fails closed with password and recovery still available`() = runTest(dispatcher) {
        val missing = Repository()
        val first = CustomerViewModel(missing, true, locker = Locker(available = false), clock = { testScheduler.currentTime })
        runCurrent(); assertEquals(0, missing.localRequests); first.begin(); runCurrent()
        assertEquals(CustomerRoute.Phone, first.state.value.route)
        val rejected = Repository()
        val second = CustomerViewModel(rejected, true, locker = Locker(approved = false), clock = { testScheduler.currentTime })
        runCurrent(); assertEquals(CustomerRoute.Phone, second.state.value.route)
        assertEquals(0, rejected.exchanges); assertEquals(1, rejected.cancellations); assertNotNull(second.state.value.message)
    }
    @Test fun `remote approval does not sign in until backend proof and stops polling in background`() = runTest(dispatcher) {
        val repo = Repository(); val model = CustomerViewModel(repo, true, clock = { testScheduler.currentTime })
        runCurrent(); model.authForeground(true); model.requestApproval("3001234567"); runCurrent()
        assertEquals(CustomerRoute.Approval, model.state.value.route); assertEquals("123", model.state.value.signinNumber)
        assertEquals(0, repo.exchanges)
        val before = repo.claims; model.authForeground(false); advanceTimeBy(15_000); runCurrent()
        assertEquals(before, repo.claims)
        repo.remoteState = DeviceClaim("approved", "t".repeat(43))
        model.authForeground(true); runCurrent()
        assertEquals(CustomerRoute.SetPassword, model.state.value.route); assertEquals(1, repo.exchanges)
        assertNull(model.state.value.signinNumber)
    }
    @Test fun `cancelled denied and expired requests never create a session`() = runTest(dispatcher) {
        val repo = Repository(); val model = CustomerViewModel(repo, true, clock = { testScheduler.currentTime })
        runCurrent(); model.authForeground(true); model.requestApproval("3001234567"); runCurrent(); model.back(); runCurrent()
        assertEquals(CustomerRoute.Phone, model.state.value.route); assertEquals(1, repo.cancellations)
        advanceTimeBy(10_000); runCurrent(); assertEquals(0, repo.exchanges)
        repo.remoteState = DeviceClaim("denied"); model.requestApproval("3001234567"); runCurrent()
        assertFalse(model.state.value.approvalPending); assertEquals(0, repo.exchanges)
        repo.remoteState = DeviceClaim("pending"); model.requestApproval("3001234567"); runCurrent()
        advanceTimeBy(180_001); runCurrent()
        assertFalse(model.state.value.approvalPending); assertEquals(0, repo.exchanges)
    }
    @Test fun `explicit logout suppresses automatic sign in on the next launch`() = runTest(dispatcher) {
        val repo = Repository().apply { deviceIdentity = DeviceIdentity("session") }; val changes = mutableListOf<Boolean>()
        val model = CustomerViewModel(repo, true, locker = Locker(), setAutoSigninAllowed = { changes += it }, clock = { testScheduler.currentTime })
        runCurrent(); model.logout(); runCurrent()
        assertEquals(false, changes.last()); assertEquals(CustomerRoute.Welcome, model.state.value.route)
        val next = Repository()
        CustomerViewModel(next, true, locker = Locker(), allowDeviceAutoSignin = false, clock = { testScheduler.currentTime })
        runCurrent(); assertEquals(0, next.localRequests)
    }

}
