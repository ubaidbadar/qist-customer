package pk.qist.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import pk.qist.customer.data.CustomerApi
import pk.qist.customer.data.CustomerApiException
import pk.qist.customer.data.CustomerOverview
import pk.qist.customer.data.CustomerSessionStore

internal enum class AuthPage { Welcome, Login, RequestPassword, ResetPassword }
internal sealed interface CustomerContent {
    data object Loading : CustomerContent
    data object SignedOut : CustomerContent
    data class Ready(val overview: CustomerOverview) : CustomerContent
    data class Failed(val message: String) : CustomerContent
}
internal data class CustomerUiState(
    val content: CustomerContent = CustomerContent.Loading, val authPage: AuthPage = AuthPage.Welcome,
    val busy: Boolean = false, val loadingMore: Boolean = false, val error: String? = null,
    val message: String? = null, val recoveryPhone: String = "",
)

internal class CustomerViewModel(private val api: CustomerApi, private val sessions: CustomerSessionStore) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerUiState())
    val state = mutableState.asStateFlow()
    private var token: String? = null
    private var requestJob: Job? = null
    private var authJob: Job? = null
    private var selectedPlan: String? = null

    init { restoreSession() }

    private fun restoreSession() {
        requestJob?.cancel()
        mutableState.update { it.copy(content = CustomerContent.Loading) }
        requestJob = viewModelScope.launch {
            try {
                token = sessions.read()
                val saved = token
                if (saved == null) mutableState.update { it.copy(content = CustomerContent.SignedOut) }
                else readOverview(saved, null)
            } catch (error: Exception) { failContent(error) }
        }
    }

    fun openAuth(page: AuthPage) {
        if (state.value.busy) return
        authJob?.cancel()
        mutableState.update { it.copy(authPage = page, busy = false, error = null, message = null) }
    }
    fun login(phone: String, password: String) = authAction {
        val session = api.login("0$phone", password)
        sessions.write(session.accessToken)
        token = session.accessToken
        mutableState.update { it.copy(busy = false, message = null) }
        refresh()
    }
    fun requestPassword(phone: String) = authAction {
        api.requestPassword("0$phone")
        mutableState.update { it.copy(authPage = AuthPage.ResetPassword, recoveryPhone = phone,
            message = "If your retailer registered this number, a code will arrive on WhatsApp. It expires in five minutes.") }
    }
    fun resetPassword(code: String, password: String) = authAction {
        api.resetPassword("0${state.value.recoveryPhone}", code, password)
        mutableState.update { it.copy(authPage = AuthPage.Login, message = "Password saved. Sign in with your phone number and new password.") }
    }
    private fun authAction(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        authJob = viewModelScope.launch {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.update { it.copy(error = messageOf(error)) }
            } finally { if (currentCoroutineContext().isActive) mutableState.update { it.copy(busy = false) } }
        }
    }
    fun refresh(contractId: String? = selectedPlan) {
        val currentToken = token
        if (currentToken == null) { restoreSession(); return }
        requestJob?.cancel()
        mutableState.update { it.copy(content = CustomerContent.Loading, loadingMore = false, error = null) }
        requestJob = viewModelScope.launch { try { readOverview(currentToken, contractId) } catch (error: Exception) { failContent(error) } }
    }
    private suspend fun readOverview(currentToken: String, contractId: String?) {
        val overview = api.overview(currentToken, contractId)
        selectedPlan = overview.plan?.id
        mutableState.update { it.copy(content = CustomerContent.Ready(overview), error = null) }
    }
    fun loadMorePayments() {
        if (state.value.loadingMore) return
        val loaded = (state.value.content as? CustomerContent.Ready)?.overview ?: return
        val plan = loaded.plan ?: return
        val cursor = loaded.payments.nextCursor ?: return
        val currentToken = token ?: return
        mutableState.update { it.copy(loadingMore = true, error = null) }
        requestJob = viewModelScope.launch {
            try {
                val next = api.payments(currentToken, plan.id, cursor)
                mutableState.update { current ->
                    val active = (current.content as? CustomerContent.Ready)?.overview
                    if (active?.plan?.id != plan.id) current else current.copy(loadingMore = false,
                        content = CustomerContent.Ready(active.copy(payments = next.copy(items = (active.payments.items + next.items).distinctBy { it.id }))))
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (error is CustomerApiException && error.status == 401) failContent(error)
                else mutableState.update { it.copy(loadingMore = false, error = messageOf(error)) }
            }
        }
    }
    fun logout() {
        if (state.value.busy) return
        requestJob?.cancel(); authJob?.cancel()
        val currentToken = token
        mutableState.update { it.copy(busy = true) }
        authJob = viewModelScope.launch {
            try {
                if (currentToken != null) {
                    try { api.logout(currentToken) }
                    catch (_: IOException) { /* Local sign-out still removes this device's token when offline. */ }
                }
                signOutLocally()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.update { it.copy(busy = false, error = messageOf(error)) }
            }
        }
    }
    private suspend fun signOutLocally() {
        sessions.write(null)
        token = null; selectedPlan = null
        mutableState.value = CustomerUiState(content = CustomerContent.SignedOut, authPage = AuthPage.Login)
    }
    private suspend fun failContent(error: Exception) {
        if (error is CancellationException) throw error
        if (error is CustomerApiException && error.status == 401) {
            try { signOutLocally() }
            catch (storageError: Exception) {
                if (storageError is CancellationException) throw storageError
                mutableState.update { it.copy(content = CustomerContent.Failed(messageOf(storageError))) }
            }
        } else mutableState.update { it.copy(content = CustomerContent.Failed(messageOf(error))) }
    }
    private fun messageOf(error: Exception): String = if (error is IOException) error.message ?: "Could not load your data. Please try again."
        else "Could not complete the request. Please try again."
}
