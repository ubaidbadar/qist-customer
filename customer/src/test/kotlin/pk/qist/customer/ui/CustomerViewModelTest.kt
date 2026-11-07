package pk.qist.customer.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        var calls = 0
        var cleared = false
        var failUnauthorized = false
        var failOffline = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun restore() = restored
        override suspend fun requestCode(phone: String) { assertEquals("03001234567", phone) }
        override suspend fun resetPassword(phone: String, code: String, password: String) = Unit
        override suspend fun login(phone: String, password: String) { calls++; gate?.await() }
        override suspend fun dashboard(contractId: String?): DashboardSample {
            calls++
            if (failUnauthorized) throw CustomerApiException(401, "Session expired")
            if (failOffline) throw java.io.IOException("offline")
            return parseOverview(overviewFixture())
        }
        override suspend fun payments(contractId: String, cursor: String): PaymentPage {
            calls++
            return PaymentPage(parseOverview(overviewFixture()).payments, null)
        }
        override suspend fun logout() { cleared = true }
        override suspend fun clearSession() { cleared = true }
    }
    @Test fun `fresh install shows welcome without overview request`() = runTest(dispatcher) {
        val repo = Repository()
        val model = CustomerViewModel(repo)
        runCurrent()
        assertEquals(CustomerRoute.Welcome, model.state.value.route)
        assertFalse(model.state.value.restoring)
        assertEquals(0, repo.calls)
    }
    @Test fun `stored session loads one overview and expires safely`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }
        val model = CustomerViewModel(repo)
        runCurrent()
        assertEquals(1, repo.calls)
        assertNotNull(model.state.value.data)
        repo.failUnauthorized = true
        model.refresh()
        runCurrent()
        assertEquals(CustomerRoute.Phone, model.state.value.route)
        assertNull(model.state.value.data)
        assertTrue(repo.cleared)
    }
    @Test fun `restored session still requires unaccepted privacy`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }
        val model = CustomerViewModel(repo, acceptedPrivacy = false)
        runCurrent()
        assertEquals(CustomerRoute.Consent, model.state.value.route)
        assertEquals(0, repo.calls)
    }
    @Test fun `duplicate sign in is ignored while request is pending`() = runTest(dispatcher) {
        val repo = Repository()
        val model = CustomerViewModel(repo)
        runCurrent()
        repo.gate = CompletableDeferred()
        model.login("3001234567", "password123", true)
        model.login("3001234567", "password123", true)
        runCurrent()
        assertEquals(1, repo.calls)
        repo.gate?.complete(Unit)
        runCurrent()
        assertEquals(2, repo.calls)
        assertEquals(CustomerRoute.Home, model.state.value.route)
    }
    @Test fun `offline refresh keeps existing account data`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }
        val model = CustomerViewModel(repo)
        runCurrent()
        val before = model.state.value.data
        repo.failOffline = true
        model.refresh()
        runCurrent()
        assertEquals(before, model.state.value.data)
        assertNotNull(model.state.value.message)
        assertFalse(model.state.value.busy)
    }
    @Test fun `paginated receipts are deduplicated by payment id`() = runTest(dispatcher) {
        val repo = Repository().apply { restored = true }
        val model = CustomerViewModel(repo)
        runCurrent()
        model.morePayments()
        runCurrent()
        assertEquals(2, model.state.value.data?.payments?.size)
        assertNull(model.state.value.data?.nextCursor)
    }
    @Test fun `WhatsApp request uses one minute resend cooldown`() = runTest(dispatcher) {
        val model = CustomerViewModel(Repository(), clock = { 1000L })
        runCurrent()
        model.requestCode("3001234567")
        runCurrent()
        assertEquals(CustomerRoute.Verify, model.state.value.route)
        assertEquals(61000L, model.state.value.deadline)
    }
}
