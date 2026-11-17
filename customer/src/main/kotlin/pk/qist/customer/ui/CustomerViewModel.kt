package pk.qist.customer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pk.qist.customer.data.CustomerApiException
import pk.qist.customer.data.CustomerRepository
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.CustomerResponseException
import pk.qist.customer.data.CustomerNetworkException
import pk.qist.customer.data.ServiceGate
import pk.qist.customer.data.LockerAuthenticator
import pk.qist.customer.data.DeviceIdentity
import pk.qist.customer.data.newSigninNonce

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
    val signinNumber: String? = null,
    val approvalPending: Boolean = false,
    val notificationStatus: String? = null,
    val serviceGate: ServiceGate? = null,
    val checkingService: Boolean = false,
)

private data class PushRegistration(val token: String?)

internal class CustomerViewModel(
    private val repository: CustomerRepository,
    private val acceptedPrivacy: Boolean,
    private val locker: LockerAuthenticator? = null,
    private val allowDeviceAutoSignin: Boolean = true,
    private val setAutoSigninAllowed: (Boolean) -> Unit = {},
    private val privacyAccepted: () -> Boolean = { acceptedPrivacy },
    private val clock: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) : ViewModel() {
    private val mutable = MutableStateFlow(CustomerState())
    val state = mutable.asStateFlow()
    private data class PendingSignin(val requestId: String, val nonce: String, val until: Long)
    private var pendingSignin: PendingSignin? = null
    private var signinPoll: Job? = null
    private var signinForeground = false
    private var setupToken: String? = null
    private var recovering = false
    private var pendingRealtime = false
    private var operation: Job? = null
    private var operationVersion = 0L
    private var backgroundOperation = false
    private val pushSync = Mutex()
    private var pushRegistration: PushRegistration? = null
    private var pushSession = 0L
    private var availabilityJob: Job? = null
    init {
        checkServiceStatus()
        perform {
            val restored = repository.restore()
            mutable.value = mutable.value.copy(restoring = false)
            if (restored) {
                mutable.value = mutable.value.copy(route = if (acceptedPrivacy) CustomerRoute.Home else CustomerRoute.Consent)
                if (acceptedPrivacy) load(null)
            } else if (allowDeviceAutoSignin && locker?.available() == true) {
                attemptLocal(privacyAccepted())
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
                if (userInitiated && gate == null && mutable.value.route == CustomerRoute.Home) refreshOverview()
            } catch (error: CancellationException) { throw error }
            catch (_: IOException) {
                if (userInitiated) mutable.value = mutable.value.copy(message = "Could not check availability. Check your internet and try again.")
            } catch (_: IllegalArgumentException) {
                if (userInitiated) mutable.value = mutable.value.copy(message = "Could not check availability. Please try again.")
            } finally { mutable.value = mutable.value.copy(checkingService = false) }
        }
    }
    private fun perform(background: Boolean = false, block: suspend () -> Unit) {
        if (mutable.value.busy) return
        if (backgroundOperation && background) return
        val previous = operation.takeIf { backgroundOperation }
        // User actions supersede automatic reads. An old coroutine must not clear a newer action's state.
        val version = ++operationVersion
        backgroundOperation = background
        previous?.cancel()
        mutable.value = mutable.value.copy(busy = !background, message = null)
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: CustomerApiException) {
                val gate = when (error.code) {
                    "MAINTENANCE_MODE" -> ServiceGate.Maintenance
                    else -> null
                }
                if (gate != null) mutable.value = mutable.value.copy(serviceGate = gate)
                if (error.status == 401 && (mutable.value.route in listOf(CustomerRoute.Home, CustomerRoute.Consent) || mutable.value.restoring)) {
                    repository.clearSession()
                    invalidatePushRegistration()
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
                val saved = mutable.value.data
                mutable.value = mutable.value.copy(data = saved?.copy(fromCache = true), message = if (saved == null)
                    "Could not connect. Check your internet and try again." else "Could not connect. Showing saved data where available.")
            } catch (_: CustomerResponseException) { invalidResponse()
            } catch (_: IOException) { mutable.value = mutable.value.copy(message = "Could not connect. Check your internet and try again.")
            } catch (_: java.security.GeneralSecurityException) { mutable.value = mutable.value.copy(message = "Could not securely save your session. Please try again.")
            } catch (_: NoSuchElementException) { invalidResponse()
            } catch (_: java.text.ParseException) { invalidResponse()
            } catch (_: IllegalArgumentException) { invalidResponse()
            } finally {
                if (version == operationVersion) {
                    operation = null
                    backgroundOperation = false
                    mutable.value = mutable.value.copy(busy = false, restoring = false, switchingPlan = false, refreshing = false)
                    if (mutable.value.route == CustomerRoute.Approval) resumeSigninPolling()
                    if (pendingRealtime) { pendingRealtime = false; if (mutable.value.route == CustomerRoute.Home) refreshOverview() }
                }
            }
        }
        operation = job
        job.start()
    }
    private fun invalidResponse() { mutable.value = mutable.value.copy(message = "Could not read the server response. Please try again.") }
    fun editPhone() { if (!mutable.value.busy) mutable.value = mutable.value.copy(phoneNotRegistered = false, message = null) }
    fun editCode() { if (!mutable.value.busy && mutable.value.codeRejected) mutable.value = mutable.value.copy(codeRejected = false, message = null) }
    fun signInAgain() { if (!mutable.value.busy) mutable.value = CustomerState(route = CustomerRoute.Phone, restoring = false) }
    fun begin() {
        if (locker == null) {
            if (!mutable.value.busy) mutable.value = mutable.value.copy(route = CustomerRoute.Phone, message = null)
        } else perform { attemptLocal(privacyAccepted()) }
    }

    private suspend fun attemptLocal(accepted: Boolean) {
        mutable.value = mutable.value.copy(route = CustomerRoute.Phone, message = null)
        val authenticator = locker ?: return
        if (!authenticator.available()) return
        val nonce = newSigninNonce()
        var requestId: String? = null
        var identity: DeviceIdentity? = null
        try {
            val challenge = repository.localChallenge(nonce)
            requestId = challenge.requestId
            if (!authenticator.authenticate(challenge.requestId, nonce)) {
                mutable.value = mutable.value.copy(message = "Quick sign-in is unavailable. Unlock your financed phone, or use password or recovery.")
                return
            }
            val claim = repository.claimDevice(challenge.requestId, nonce)
            val ticket = claim.ticket ?: throw CustomerResponseException("The phone did not approve sign-in.")
            identity = repository.exchangeDevice(ticket, nonce)
            requestId = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            mutable.value = mutable.value.copy(message = "Quick sign-in could not be completed. Use password or account recovery.")
        } catch (_: SecurityException) {
            mutable.value = mutable.value.copy(message = "Trusted phone sign-in is unavailable. Use password or account recovery.")
        } finally {
            val abandoned = requestId
            if (abandoned != null) {
                try { repository.cancelDevice(abandoned, nonce) }
                catch (error: CancellationException) { throw error }
                catch (_: IOException) { /* The unclaimed request also expires on the backend. */ }
            }
        }
        identity?.let { applyIdentity(it, accepted) }
    }

    private suspend fun applyIdentity(identity: DeviceIdentity, accepted: Boolean) {
        when (identity.next) {
            "session" -> signedIn(accepted)
            "password" -> {
                setupToken = requireNotNull(identity.setupToken)
                mutable.value = mutable.value.copy(phone = requireNotNull(identity.phone).removePrefix("0"), route = CustomerRoute.SetPassword, signinNumber = null, approvalPending = false)
            }
            else -> throw CustomerResponseException("Invalid device sign-in response.")
        }
    }

    fun requestApproval(phone: String) {
        if (!phone.matches(Regex("3[0-9]{9}"))) {
            mutable.value = mutable.value.copy(message = "Enter a valid mobile number starting with 3.")
            return
        }
        perform {
            stopSigninPolling()
            setupToken = null
            val nonce = newSigninNonce()
            val challenge = repository.remoteChallenge("0$phone", nonce)
            pendingSignin = PendingSignin(challenge.requestId, nonce, clock() + 180_000)
            mutable.value = mutable.value.copy(route = CustomerRoute.Approval, phone = phone, signinNumber = challenge.number, approvalPending = true)
        }
    }

    fun authForeground(active: Boolean) {
        signinForeground = active
        if (active) resumeSigninPolling() else stopSigninPolling()
    }

    private fun stopSigninPolling() { signinPoll?.cancel(); signinPoll = null }

    private fun resumeSigninPolling() {
        if (!signinForeground || mutable.value.busy || signinPoll?.isActive == true || pendingSignin == null) return
        signinPoll = viewModelScope.launch {
            val pending = pendingSignin ?: return@launch
            while (pendingSignin == pending && mutable.value.route == CustomerRoute.Approval && clock() < pending.until) {
                try {
                    val claim = repository.claimDevice(pending.requestId, pending.nonce)
                    if (pendingSignin != pending) return@launch
                    when (claim.state) {
                        "approved" -> {
                            pendingSignin = null
                            signinPoll = null
                            mutable.value = mutable.value.copy(approvalPending = false, signinNumber = null, message = null)
                            perform {
                                applyIdentity(repository.exchangeDevice(requireNotNull(claim.ticket), pending.nonce), privacyAccepted())
                            }
                            return@launch
                        }
                        "denied" -> {
                            pendingSignin = null
                            mutable.value = mutable.value.copy(approvalPending = false, message = "The financed phone declined this request. Use password or recovery.")
                            return@launch
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: CustomerApiException) {
                    if (error.status in listOf(400, 401, 403, 409)) {
                        pendingSignin = null
                        mutable.value = mutable.value.copy(approvalPending = false, message = "This sign-in request is no longer valid. Use password or recovery.")
                        return@launch
                    }
                    mutable.value = mutable.value.copy(message = "Waiting for a secure connection. You can also use password or recovery.")
                } catch (_: IOException) {
                    mutable.value = mutable.value.copy(message = "Waiting for a secure connection. You can also use password or recovery.")
                }
                delay(5_000)
            }
            if (pendingSignin == pending) {
                pendingSignin = null
                mutable.value = mutable.value.copy(approvalPending = false, message = "This request expired. Use password or request sign-in again.")
            }
        }
    }

    private fun abandonSignin() {
        stopSigninPolling()
        val previous = pendingSignin
        pendingSignin = null
        mutable.value = mutable.value.copy(approvalPending = false, signinNumber = null)
        if (previous != null) viewModelScope.launch {
            try { repository.cancelDevice(previous.requestId, previous.nonce) }
            catch (error: CancellationException) { throw error }
            catch (_: IOException) { /* Server expiration is the backstop when offline. */ }
        }
    }

    fun usePassword(phone: String) {
        if (mutable.value.busy || !phone.matches(Regex("3[0-9]{9}"))) return
        abandonSignin()
        mutable.value = mutable.value.copy(route = CustomerRoute.Password, phone = phone, message = null)
    }

    fun useWhatsApp(phone: String) {
        if (mutable.value.busy || !phone.matches(Regex("3[0-9]{9}"))) return
        abandonSignin()
        mutable.value = mutable.value.copy(phone = phone)
        requestCode(true)
    }
    fun back() {
        if (mutable.value.busy) return
        abandonSignin()
        setupToken = null
        val next = when (mutable.value.route) {
            CustomerRoute.Password, CustomerRoute.Verify, CustomerRoute.SetPassword, CustomerRoute.Approval -> CustomerRoute.Phone
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
        setAutoSigninAllowed(true)
        invalidatePushRegistration()
        mutable.value = mutable.value.copy(route = if (accepted) CustomerRoute.Home else CustomerRoute.Consent, data = null)
        if (accepted) load(null)
    }
    fun acceptPrivacy() = perform { mutable.value = mutable.value.copy(route = CustomerRoute.Home); load(null) }
    fun declinePrivacy() = perform { setAutoSigninAllowed(false); repository.clearSession(); invalidatePushRegistration(); mutable.value = CustomerState(restoring = false) }
    fun deleteAccount() = perform { setAutoSigninAllowed(false); repository.deleteAccount(); invalidatePushRegistration(); setupToken = null; mutable.value = CustomerState(restoring = false) }
    fun logout() = perform { setAutoSigninAllowed(false); repository.logout(); invalidatePushRegistration(); mutable.value = CustomerState(restoring = false) }
    private suspend fun load(id: String?) {
        val data = repository.dashboard(id)
        mutable.value = mutable.value.copy(data = data)
    }
    private fun invalidatePushRegistration() { pushSession++; pushRegistration = null }

    /** One applied registration per session, retained across Activity recreation and serialized across callers. */
    suspend fun syncPushToken(token: String?): Boolean = pushSync.withLock {
        if (mutable.value.route != CustomerRoute.Home) return@withLock false
        if (pushRegistration != null && pushRegistration?.token == token) return@withLock true
        val session = pushSession
        try {
            if (token == null) repository.unregisterPush() else repository.registerPush(token)
            if (session != pushSession || mutable.value.route != CustomerRoute.Home) return@withLock false
            pushRegistration = PushRegistration(token)
            mutable.value = mutable.value.copy(notificationStatus = null)
            true
        } catch (error: CancellationException) { throw error }
        catch (_: IOException) { if (session == pushSession && mutable.value.route == CustomerRoute.Home) pushUnavailable(); false }
    }
    fun pushUnavailable() { mutable.value = mutable.value.copy(notificationStatus = "Notifications could not connect. They will retry when you reopen the app.") }
    fun onForeground() { if (mutable.value.route == CustomerRoute.Home && !mutable.value.busy && mutable.value.data != null) refreshOverview() }
    fun onRealtimeChange() { if (mutable.value.route != CustomerRoute.Home) return; if (mutable.value.busy || backgroundOperation) pendingRealtime = true else refreshOverview() }
    fun markNotificationsRead(ids: List<String>) = perform {
        val unread = ids.take(30)
        if (unread.isNotEmpty()) {
            repository.readNotifications(unread)
            mutable.value = mutable.value.copy(data = mutable.value.data?.let { data -> data.copy(notifications = data.notifications.map { if (it.id in unread) it.copy(read = true) else it }) })
        }
    }
    fun openNotification(id: String) {
        val current = mutable.value.data ?: return
        val notice = current.notifications.firstOrNull { it.id == id } ?: return
        if (notice.contractId == current.id) {
            // The destination already has the requested device's overview.
            // Only acknowledge the notification; navigating tabs needs no dashboard request.
            if (!notice.read) markNotificationsRead(listOf(id))
            return
        }
        perform {
            mutable.value = mutable.value.copy(switchingPlan = true)
            if (!notice.read) repository.readNotifications(listOf(id))
            load(notice.contractId)
        }
    }
    /** Only a user pull requests the top indicator; resume and push updates keep content visible. */
    fun refresh() = refreshOverview(showIndicator = true)
    private fun refreshOverview(showIndicator: Boolean = false) {
        if (mutable.value.route != CustomerRoute.Home) return
        perform(background = !showIndicator && mutable.value.data != null) {
            mutable.value = mutable.value.copy(refreshing = showIndicator)
            load(mutable.value.data?.id)
        }
    }
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
