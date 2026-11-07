package pk.qist.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pk.qist.customer.data.CustomerApiException
import pk.qist.customer.data.CustomerRepository
import pk.qist.customer.data.DashboardSample
import java.io.IOException

internal data class CustomerState(
    val route: CustomerRoute = CustomerRoute.Welcome,
    val busy: Boolean = false,
    val restoring: Boolean = true,
    val data: DashboardSample? = null,
    val message: String? = null,
    val deadline: Long = 0L,
    val resetComplete: Boolean = false,
)

internal class CustomerViewModel(private val repository: CustomerRepository, private val acceptedPrivacy: Boolean = true, private val clock: () -> Long = { android.os.SystemClock.elapsedRealtime() }) : ViewModel() {
    private val mutable = MutableStateFlow(CustomerState())
    val state = mutable.asStateFlow()
    init {
        perform {
            if (repository.restore()) {
                mutable.value = mutable.value.copy(route = if (acceptedPrivacy) CustomerRoute.Home else CustomerRoute.Consent)
                if (acceptedPrivacy) load()
            }
        }
    }
    private fun perform(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: CustomerApiException) {
                if (e.status == 401 && mutable.value.route == CustomerRoute.Home) {
                    repository.clearSession()
                    mutable.value = mutable.value.copy(route = CustomerRoute.Phone, data = null)
                }
                mutable.value = mutable.value.copy(message = e.message)
            }
            catch (_: IOException) { mutable.value = mutable.value.copy(message = "Could not connect. Check your internet and try again.") }
            catch (_: NoSuchElementException) { mutable.value = mutable.value.copy(message = "The server response is incomplete. Please try again.") }
            catch (_: java.text.ParseException) { mutable.value = mutable.value.copy(message = "The server returned an invalid date. Please try again.") }
            catch (_: java.security.GeneralSecurityException) { mutable.value = mutable.value.copy(message = "Could not secure your session. Please try again.") }
            catch (_: IllegalArgumentException) { mutable.value = mutable.value.copy(message = "Could not read the server response. Please try again.") }
            finally { mutable.value = mutable.value.copy(busy = false, restoring = false) }
        }
    }
    fun navigate(route: CustomerRoute) {
        if (!mutable.value.busy) mutable.value = mutable.value.copy(route = route, message = null, resetComplete = false)
    }
    fun requestCode(phone: String) = perform {
        repository.requestCode("0$phone")
        mutable.value = mutable.value.copy(route = CustomerRoute.Verify, deadline = clock() + 60_000)
    }
    fun reset(phone: String, code: String, password: String) = perform {
        repository.resetPassword("0$phone", code, password)
        mutable.value = mutable.value.copy(route = CustomerRoute.Phone, resetComplete = true)
    }
    fun login(phone: String, password: String, acceptedPrivacy: Boolean) = perform {
        repository.login("0$phone", password)
        mutable.value = mutable.value.copy(route = if (acceptedPrivacy) CustomerRoute.Home else CustomerRoute.Consent)
        if (acceptedPrivacy) load()
    }
    fun acceptPrivacy() = perform {
        mutable.value = mutable.value.copy(route = CustomerRoute.Home)
        load()
    }
    fun logout() = perform {
        repository.logout()
        mutable.value = CustomerState(restoring = false)
    }
    fun declinePrivacy() = perform {
        repository.clearSession()
        mutable.value = CustomerState(restoring = false)
    }
    private suspend fun load(contractId: String? = mutable.value.data?.contractId) {
        val data = repository.dashboard(contractId)
        mutable.value = mutable.value.copy(data = data)
    }
    fun refresh() = perform { load() }
    fun selectPlan(id: String) = perform { load(id) }
    fun morePayments() {
        val current = mutable.value.data ?: return
        val id = current.contractId ?: return
        val cursor = current.nextCursor ?: return
        perform {
            val page = repository.payments(id, cursor)
            mutable.value = mutable.value.copy(data = current.copy(
                payments = (current.payments + page.items).distinctBy { it.id }, nextCursor = page.nextCursor,
            ))
        }
    }
}
