package pk.qist.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pk.qist.customer.data.CustomerApiException
import pk.qist.customer.data.CustomerRepository
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.CustomerNetworkException
import pk.qist.customer.data.ServiceGate

internal data class CustomerState(
    val route: CustomerRoute = CustomerRoute.Welcome,
    val phone: String = "",
    val busy: Boolean = false,
    val restoring: Boolean = true,
    val refreshing: Boolean = false,
    val switchingPlan: Boolean = false,
    val data: DashboardSample? = null,
    val message: String? = null,
    val deadline: Long = 0L,
    val verifyBlockedUntil: Long = 0L,
    val phoneNotRegistered: Boolean = false,
    val codeRejected: Boolean = false,
    val notificationStatus: String? = null,
    val serviceGate: ServiceGate? = null,
    val checkingService: Boolean = false,
)

internal class CustomerViewModel(
    private val repository: CustomerRepository,
    private val acceptedPrivacy: Boolean,
    private val clock: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) : ViewModel() {
    private val mutable = MutableStateFlow(CustomerState())
    val state = mutable.asStateFlow()
    private var setupToken: String? = null
    private var recovering = false
    private var pendingRealtime = false
    private var availabilityJob: Job? = null
    init {
        checkServiceStatus()
        perform {
            val restored = repository.restore()
            mutable.value = mutable.value.copy(restoring = false)
            if (restored) {
                mutable.value = mutable.value.copy(route = if (acceptedPrivacy) CustomerRoute.Home else CustomerRoute.Consent)
                if (acceptedPrivacy) load(null)
            }
        }
    }
    fun checkServiceStatus(userInitiated: Boolean = false) {
        availabilityJob?.cancel()
        availabilityJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(checkingService = true)
            try {
                val availability = repository.availability()
                val gate = availability.gate()
                if (gate != null || userInitiated) mutable.value = mutable.value.copy(serviceGate = gate, message = null)
                if (userInitiated && gate == null && mutable.value.route == CustomerRoute.Home) refresh()
            } catch (error: CancellationException) { throw error }
            catch (_: IOException) {
                if (userInitiated) mutable.value = mutable.value.copy(message = "Could not check availability. Check your internet and try again.")
            } catch (_: IllegalArgumentException) {
                if (userInitiated) mutable.value = mutable.value.copy(message = "Could not check availability. Please try again.")
            } finally { mutable.value = mutable.value.copy(checkingService = false) }
        }
    }
    private fun perform(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try { block() } catch (error: CancellationException) { throw error } catch (error: CustomerApiException) {
                val gate = when (error.code) {
                    "MAINTENANCE_MODE" -> ServiceGate.Maintenance
                    else -> null
                }
                if (gate != null) mutable.value = mutable.value.copy(serviceGate = gate)
                if (error.status == 401 && (mutable.value.route in listOf(CustomerRoute.Home, CustomerRoute.Consent) || mutable.value.restoring)) {
                    repository.clearSession()
                    mutable.value = mutable.value.copy(route = CustomerRoute.SessionExpired, data = null, phone = "")
                }
                if (mutable.value.route == CustomerRoute.Verify && error.status == 429 && error.retryAfterSeconds > 0) {
                    mutable.value = mutable.value.copy(verifyBlockedUntil = clock() + error.retryAfterSeconds * 1000,
                        codeRejected = false)
                } else if (mutable.value.route == CustomerRoute.Verify && error.code == "OTP_CODE_INVALID") {
                    mutable.value = mutable.value.copy(codeRejected = true)
                }
                mutable.value = mutable.value.copy(message = error.message)
            } catch (_: CustomerNetworkException) {
                mutable.value = mutable.value.copy(data = mutable.value.data?.copy(fromCache = true), message = "Could not connect. Showing saved data where available.")
            } catch (_: IOException) { mutable.value = mutable.value.copy(message = "Could not connect. Check your internet and try again.") } catch (_: java.security.GeneralSecurityException) { mutable.value = mutable.value.copy(message = "Could not securely save your session. Please try again.") } catch (_: NoSuchElementException) { invalidResponse() } catch (_: java.text.ParseException) { invalidResponse() } catch (_: IllegalArgumentException) { invalidResponse() } finally { mutable.value = mutable.value.copy(busy = false, restoring = false, switchingPlan = false, refreshing = false)
                if (pendingRealtime) { pendingRealtime = false; if (mutable.value.route == CustomerRoute.Home) refresh() }
            }
        }
    }
    private fun invalidResponse() { mutable.value = mutable.value.copy(message = "Could not read the server response. Please try again.") }
    fun editPhone() { if (!mutable.value.busy) mutable.value = mutable.value.copy(phoneNotRegistered = false, message = null) }
    fun editCode() { if (!mutable.value.busy && mutable.value.codeRejected) mutable.value = mutable.value.copy(codeRejected = false, message = null) }
    fun signInAgain() { if (!mutable.value.busy) mutable.value = CustomerState(route = CustomerRoute.Phone, restoring = false) }
    fun begin() { if (!mutable.value.busy) mutable.value = mutable.value.copy(route = CustomerRoute.Phone, message = null) }
    fun back() {
        if (mutable.value.busy) return
        setupToken = null
        val next = when (mutable.value.route) {
            CustomerRoute.Password, CustomerRoute.Verify, CustomerRoute.SetPassword -> CustomerRoute.Phone
            else -> CustomerRoute.Welcome
        }
        mutable.value = mutable.value.copy(route = next, message = null)
    }
    fun start(phone: String) {
        if (!phone.matches(Regex("3[0-9]{9}"))) {
            mutable.value = mutable.value.copy(message = "Enter a valid mobile number starting with 3.")
            return
        }
        if (mutable.value.phoneNotRegistered) return
        perform {
            setupToken = null; recovering = false
            mutable.value = mutable.value.copy(phone = phone, phoneNotRegistered = false, codeRejected = false, verifyBlockedUntil = 0)
            val result = repository.start("0$phone")
            mutable.value = when (result.next) {
                "password" -> mutable.value.copy(route = CustomerRoute.Password)
                "otp" -> mutable.value.copy(route = CustomerRoute.Verify, deadline = clock() + result.retryAfterSeconds * 1000L, codeRejected = false, verifyBlockedUntil = 0)
                else -> mutable.value.copy(route = CustomerRoute.Phone, phoneNotRegistered = true, message = "No plan is linked to this number. Ask your retailer to check your registered number.")
            }
        }
    }
    fun requestCode(recovery: Boolean = recovering) {
        if (clock() < mutable.value.verifyBlockedUntil) return
        perform {
            if (recovery) {
                val cooldown = repository.requestCode("0${mutable.value.phone}")
                setupToken = null; recovering = true
                mutable.value = mutable.value.copy(route = CustomerRoute.Verify, deadline = clock() + cooldown * 1000L, codeRejected = false, verifyBlockedUntil = 0)
            } else {
                val result = repository.start("0${mutable.value.phone}")
                setupToken = null; recovering = false
                mutable.value = when (result.next) {
                    "otp" -> mutable.value.copy(route = CustomerRoute.Verify, deadline = clock() + result.retryAfterSeconds * 1000L, codeRejected = false, verifyBlockedUntil = 0)
                    "password" -> mutable.value.copy(route = CustomerRoute.Password)
                    else -> mutable.value.copy(route = CustomerRoute.Phone, phoneNotRegistered = true, message = "No plan is linked to this number. Contact your retailer.")
                }
            }
        }
    }
    fun verify(code: String) {
        if (clock() < mutable.value.verifyBlockedUntil) return
        if (!code.matches(Regex("[0-9]{6}"))) return
        perform {
            setupToken = repository.verifyCode("0${mutable.value.phone}", code)
            mutable.value = mutable.value.copy(route = CustomerRoute.SetPassword)
        }
    }
    fun login(password: String, accepted: Boolean) = perform {
        repository.login("0${mutable.value.phone}", password)
        signedIn(accepted)
    }
    fun savePassword(password: String, accepted: Boolean) {
        val proof = setupToken ?: return
        perform {
            repository.setPassword("0${mutable.value.phone}", proof, password)
            setupToken = null
            signedIn(accepted)
        }
    }
    private suspend fun signedIn(accepted: Boolean) {
        mutable.value = mutable.value.copy(route = if (accepted) CustomerRoute.Home else CustomerRoute.Consent, data = null)
        if (accepted) load(null)
    }
    fun acceptPrivacy() = perform { mutable.value = mutable.value.copy(route = CustomerRoute.Home); load(null) }
    fun declinePrivacy() = perform { repository.clearSession(); mutable.value = CustomerState(restoring = false) }
    fun deleteAccount() = perform { repository.deleteAccount(); setupToken = null; mutable.value = CustomerState(restoring = false) }
    fun logout() = perform { repository.logout(); mutable.value = CustomerState(restoring = false) }
    private suspend fun load(id: String?) {
        val data = repository.dashboard(id)
        mutable.value = mutable.value.copy(data = data)
    }
    suspend fun syncPushToken(token: String?): Boolean {
        return try { if (token == null) repository.unregisterPush() else repository.registerPush(token); mutable.value = mutable.value.copy(notificationStatus = null); true }
        catch (error: CancellationException) { throw error }
        catch (_: IOException) { pushUnavailable(); false }
    }
    fun pushUnavailable() { mutable.value = mutable.value.copy(notificationStatus = "Notifications could not connect. They will retry when you reopen the app.") }
    fun onForeground() { if (!mutable.value.busy && mutable.value.data != null) refresh() }
    fun onRealtimeChange() { if (mutable.value.route != CustomerRoute.Home) return; if (mutable.value.busy) pendingRealtime = true else refresh() }
    fun markNotificationsRead(ids: List<String>) = perform {
        val unread = ids.take(30)
        if (unread.isNotEmpty()) {
            repository.readNotifications(unread)
            mutable.value = mutable.value.copy(data = mutable.value.data?.let { data -> data.copy(notifications = data.notifications.map { if (it.id in unread) it.copy(read = true) else it }) })
        }
    }
    fun openNotification(id: String) {
        val notice = mutable.value.data?.notifications?.firstOrNull { it.id == id } ?: return
        perform {
            mutable.value = mutable.value.copy(switchingPlan = true)
            repository.readNotifications(listOf(id))
            load(notice.contractId)
        }
    }
    fun refresh() = perform { mutable.value = mutable.value.copy(refreshing = true); load(mutable.value.data?.id) }
    fun selectPlan(id: String) {
        if (id == mutable.value.data?.id || mutable.value.busy) return
        perform { mutable.value = mutable.value.copy(switchingPlan = true); load(id) }
    }
    fun morePayments() {
        val current = mutable.value.data ?: return
        val id = current.id ?: return
        val cursor = current.nextCursor ?: return
        perform {
            val page = repository.payments(id, cursor)
            mutable.value = mutable.value.copy(data = current.copy(payments = (current.payments + page.items).distinctBy { it.id }, nextCursor = page.nextCursor))
        }
    }
}
