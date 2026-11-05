package pk.qist.customer

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pk.qist.customer.data.*
import pk.qist.customer.ui.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val dispatcher = StandardTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

private val overview = CustomerOverview("A Customer", "03001234567", emptyList(), null, PaymentPage(emptyList(), null), "2026-11-05")
private class MemorySession(var token: String? = null) : CustomerSessionStore {
    var blockRead: CompletableDeferred<Unit>? = null
    override suspend fun read(): String? { blockRead?.await(); return token }
    override suspend fun write(token: String?) { this.token = token }
}
private class FakeApi : CustomerApi {
    var reads = 0
    var loginPhone = ""
    var failure: Exception? = null
    var blockLogin: CompletableDeferred<Unit>? = null
    var blockLogout: CompletableDeferred<Unit>? = null
    var blockOverview: CompletableDeferred<Unit>? = null
    var selectionDelay = false
    override suspend fun login(phone: String, password: String): CustomerSession { loginPhone = phone; blockLogin?.await(); failure?.let { throw it }; return CustomerSession("signed-token", "2026-12-05") }
    override suspend fun requestPassword(phone: String) { failure?.let { throw it } }
    override suspend fun resetPassword(phone: String, code: String, password: String) { failure?.let { throw it } }
    override suspend fun overview(token: String, contractId: String?): CustomerOverview {
        reads++; blockOverview?.await(); if (selectionDelay && contractId == "slow") delay(1000)
        failure?.let { throw it }
        return overview.copy(name = contractId ?: overview.name)
    }
    override suspend fun payments(token: String, contractId: String, cursor: String) = PaymentPage(emptyList(), null)
    override suspend fun logout(token: String) { blockLogout?.await(); failure?.let { throw it } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @Test fun firstLaunchWaitsForLocalSessionThenShowsWelcomeWithoutFetchingAccount() = runTest {
        val api = FakeApi(); val store = MemorySession(); store.blockRead = CompletableDeferred()
        val model = CustomerViewModel(api, store)
        assertEquals(CustomerContent.RestoringSession, model.state.value.content)
        runCurrent()
        assertEquals(CustomerContent.RestoringSession, model.state.value.content)
        assertEquals(0, api.reads)
        store.blockRead?.complete(Unit); advanceUntilIdle()
        assertEquals(CustomerContent.SignedOut, model.state.value.content)
        assertEquals(AuthPage.Welcome, model.state.value.authPage)
        assertEquals(0, api.reads)
    }
    @Test fun savedSessionShowsAccountLoadingOnlyAfterTheTokenIsRestored() = runTest {
        val api = FakeApi(); api.blockOverview = CompletableDeferred()
        val store = MemorySession("saved-token"); store.blockRead = CompletableDeferred()
        val model = CustomerViewModel(api, store); runCurrent()
        assertEquals(CustomerContent.RestoringSession, model.state.value.content)
        assertEquals(0, api.reads)
        store.blockRead?.complete(Unit); runCurrent()
        assertEquals(CustomerContent.Loading, model.state.value.content)
        assertEquals(1, api.reads)
        api.blockOverview?.complete(Unit); advanceUntilIdle()
        assertTrue(model.state.value.content is CustomerContent.Ready)
        assertEquals(1, api.reads)
    }
    @Test fun loginPersistsTokenAndLoadsOneSharedOverview() = runTest {
        val api = FakeApi(); val store = MemorySession(); val model = CustomerViewModel(api, store)
        advanceUntilIdle(); assertEquals(CustomerContent.SignedOut, model.state.value.content)
        model.login("3001234567", "secret-password"); advanceUntilIdle()
        assertEquals("03001234567", api.loginPhone); assertEquals("signed-token", store.token)
        assertEquals(1, api.reads); assertTrue(model.state.value.content is CustomerContent.Ready)
        assertFalse(model.state.value.busy)
    }
    @Test fun wrongPasswordDoesNotPersistAnySessionAndDuplicateSubmissionIsBlocked() = runTest {
        val api = FakeApi(); val store = MemorySession(); val model = CustomerViewModel(api, store); advanceUntilIdle()
        api.failure = CustomerApiException(401, "Phone number or password is incorrect")
        model.login("3001234567", "wrong-password"); advanceUntilIdle()
        assertNull(store.token); assertEquals(0, api.reads); assertNotNull(model.state.value.error)
        api.failure = null; api.blockLogin = CompletableDeferred()
        model.login("3001234567", "secret-password"); model.login("3111234567", "secret-password"); runCurrent()
        assertEquals("03001234567", api.loginPhone)
        api.blockLogin?.complete(Unit); advanceUntilIdle(); assertEquals(1, api.reads)
    }
    @Test fun expiredSessionSignsOutWhileNetworkFailureAllowsRetry() = runTest {
        val api = FakeApi(); val store = MemorySession("old-token"); api.failure = IOException("Offline")
        val model = CustomerViewModel(api, store); advanceUntilIdle()
        assertTrue(model.state.value.content is CustomerContent.Failed); assertEquals("old-token", store.token)
        api.failure = null; model.refresh(); advanceUntilIdle(); assertTrue(model.state.value.content is CustomerContent.Ready)
        api.failure = CustomerApiException(401, "Expired"); model.refresh(); advanceUntilIdle()
        assertEquals(CustomerContent.SignedOut, model.state.value.content); assertNull(store.token)
    }
    @Test fun supersededPlanRequestCannotOverwriteNewerSelection() = runTest {
        val api = FakeApi(); api.selectionDelay = true
        val model = CustomerViewModel(api, MemorySession("token")); advanceUntilIdle()
        model.refresh("slow"); runCurrent(); model.refresh("newer"); advanceUntilIdle()
        assertEquals("newer", (model.state.value.content as CustomerContent.Ready).overview.name)
    }
    @Test fun recoveryOnlyMovesForwardAfterServerSuccessAndLogoutClearsLocalTokenOffline() = runTest {
        val api = FakeApi(); val store = MemorySession(); val model = CustomerViewModel(api, store); advanceUntilIdle()
        model.requestPassword("3001234567"); advanceUntilIdle(); assertEquals(AuthPage.ResetPassword, model.state.value.authPage)
        model.resetPassword("123456", "new-password"); advanceUntilIdle(); assertEquals(AuthPage.Login, model.state.value.authPage)
        model.login("3001234567", "new-password"); advanceUntilIdle()
        api.failure = IOException("Offline"); model.logout(); advanceUntilIdle()
        assertNull(store.token); assertEquals(CustomerContent.SignedOut, model.state.value.content)
    }
    @Test fun logoutBlocksNewReadsUntilTheLocalSessionIsCleared() = runTest {
        val api = FakeApi(); val store = MemorySession("token"); val model = CustomerViewModel(api, store)
        advanceUntilIdle(); val reads = api.reads
        api.blockLogout = CompletableDeferred()
        model.logout(); runCurrent(); model.refresh("should-not-start"); runCurrent()
        assertEquals(reads, api.reads)
        api.blockLogout?.complete(Unit); advanceUntilIdle()
        assertNull(store.token); assertEquals(CustomerContent.SignedOut, model.state.value.content)
    }
}
