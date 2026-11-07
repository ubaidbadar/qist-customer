package pk.qist.customer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pk.qist.customer.platform.CustomerPushBinding
import pk.qist.customer.platform.CustomerUpdateHost
import pk.qist.customer.platform.PhoneNumberHint
import pk.qist.customer.BuildConfig
import pk.qist.customer.data.*
import pk.qist.customer.ui.auth.*
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.dashboard.*
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.payments.HowToPay
import pk.qist.customer.ui.profile.PrivacyPolicy
import pk.qist.customer.ui.profile.ServiceAvailabilityScreen

internal enum class CustomerRoute { Welcome, Phone, Password, Verify, SetPassword, Consent, Home, SessionExpired }
internal enum class CustomerTab { Home, Payments, Plan, Profile }

@Composable
internal fun CustomerShell() {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { CustomerPreferences(context) }
    val model = viewModel {
        val storage = KeystoreTextStore(context)
        CustomerViewModel(HttpCustomerRepository(CustomerSessionStore(storage), BuildConfig.CUSTOMER_API_URL,
            overviewCache = BoundOverviewCache(storage), appVersionCode = BuildConfig.VERSION_CODE.toLong()),
            preferences.hasAcceptedPrivacy)
    }
    val state by model.state.collectAsStateWithLifecycle()
    CustomerPushBinding(state.route == CustomerRoute.Home, preferences, model)
    var tab by rememberSaveable { mutableStateOf(CustomerTab.Home) }
    var urdu by rememberSaveable { mutableStateOf(preferences.urdu) }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var paymentHelp by rememberSaveable { mutableStateOf(false) }
    var consentPolicy by rememberSaveable { mutableStateOf(false) }
    val openNotificationInbox by pk.qist.customer.platform.CustomerPushSignals.openInbox.collectAsStateWithLifecycle()
    LaunchedEffect(openNotificationInbox, state.route) { if (openNotificationInbox && state.route == CustomerRoute.Home) { paymentHelp = false; tab = CustomerTab.Home } }
    LaunchedEffect(state.verifyBlockedUntil) { if (state.verifyBlockedUntil > android.os.SystemClock.elapsedRealtime()) code = "" }
    LaunchedEffect(state.route) {
        password = ""; code = ""
        if (state.route == CustomerRoute.Welcome || state.route == CustomerRoute.SessionExpired) { tab = CustomerTab.Home; paymentHelp = false; phone = "" }
    }
    BackHandler(state.serviceGate == null && state.route in listOf(CustomerRoute.Phone, CustomerRoute.Password, CustomerRoute.Verify, CustomerRoute.SetPassword), onBack = model::back)
    BackHandler(state.serviceGate == null && state.route == CustomerRoute.Home && tab != CustomerTab.Home) { tab = CustomerTab.Home }
    BackHandler(state.serviceGate == null && state.route == CustomerRoute.Consent) { if (consentPolicy) consentPolicy = false else model.declinePrivacy() }
    BackHandler(state.serviceGate == null && paymentHelp) { paymentHelp = false }
    CompositionLocalProvider(LocalUrdu provides urdu, LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        CustomerUpdateHost(enabled = state.route == CustomerRoute.Home && state.serviceGate == null) {
        val gate = state.serviceGate
        if (state.restoring) {
            AccountSplash()
        } else if (gate != null) {
            ServiceAvailabilityScreen(state.checkingService, state.message) { model.checkServiceStatus(true) }
        } else when (state.route) {
            CustomerRoute.SessionExpired -> SessionExpired(model::signInAgain)
            CustomerRoute.Welcome -> Welcome({ urdu = !urdu; preferences.setUrdu(urdu) }, model::begin)
            CustomerRoute.Phone -> AuthScreen(
                copy("Enter your phone number", "اپنا فون نمبر درج کریں"),
                copy("Use the number registered with your retailer.", "اپنے دکاندار کے پاس رجسٹرڈ نمبر استعمال کریں۔"),
                copy("Continue", "جاری رکھیں"), model::back, { model.start(phone) },
                footer = copy("Not registered? Contact your retailer.", "رجسٹرڈ نہیں؟ دکاندار سے رابطہ کریں۔"), busy = state.busy,
                enabled = phone.matches(Regex("3[0-9]{9}")) && !state.phoneNotRegistered,
            ) {
                PhoneField(phone, state.phoneNotRegistered) { if (!state.busy) { phone = normalizePhone(it); model.editPhone() } }
                PhoneNumberHint(phone.isEmpty(), !state.busy) { phone = it; model.editPhone() }
                AuthError(state.message)
            }
            CustomerRoute.Password, CustomerRoute.SetPassword -> {
                val creating = state.route == CustomerRoute.SetPassword
                AuthScreen(
                    if (creating) copy("Create your password", "اپنا پاس ورڈ بنائیں") else copy("Welcome back", "واپس خوش آمدید"),
                    if (creating) copy("Your number is verified. Choose a password with 8–128 characters.", "نمبر کی تصدیق ہو گئی۔ ۸ تا ۱۲۸ حروف کا پاس ورڈ بنائیں۔") else copy("Enter your password for +92 ${state.phone}.", "+92 ${state.phone} کا پاس ورڈ درج کریں۔"),
                    if (creating) copy("Save and continue", "محفوظ کریں اور جاری رکھیں") else copy("Sign in", "لاگ ان کریں"),
                    model::back, { if (creating) model.savePassword(password, preferences.hasAcceptedPrivacy) else model.login(password, preferences.hasAcceptedPrivacy) },
                    busy = state.busy, enabled = password.length in 8..128,
                ) {
                    PasswordField(password, creating) { if (!state.busy) password = it.take(128) }
                    TextButton(onClick = { model.requestCode(true) }, enabled = !state.busy) {
                        Text(if (creating) copy("Verify again", "دوبارہ تصدیق کریں") else copy("Forgot password?", "پاس ورڈ بھول گئے؟"))
                    }
                    AuthError(state.message)
                }
            }
            CustomerRoute.Verify -> {
                val blocked = remainingSeconds(state.verifyBlockedUntil)
                AuthScreen(copy("Verify your number", "اپنے نمبر کی تصدیق کریں"),
                    copy("Enter the 6-digit code sent to +92 ${state.phone} on WhatsApp.", "+92 ${state.phone} کے واٹس ایپ پر بھیجا گیا ۶ ہندسوں کا کوڈ درج کریں۔"),
                    copy("Verify", "تصدیق کریں"), model::back, { model.verify(code) }, busy = state.busy,
                    enabled = code.length == 6 && blocked == 0L) {
                    CodeField(code, error = state.codeRejected, enabled = !state.busy && blocked == 0L) { code = it; model.editCode() }
                    if (blocked > 0) VerificationWait(blocked) else {
                        AuthError(state.message)
                        Resend(state.deadline, { code = ""; model.requestCode() }, enabled = !state.busy)
                    }
                }
            }
            CustomerRoute.Consent -> if (consentPolicy) {
                AuthScreen(copy("Privacy policy", "رازداری کی پالیسی"), "", copy("Back", "واپس"), { consentPolicy = false }, { consentPolicy = false }) { PrivacyPolicy() }
            } else PrivacyConsent({ consentPolicy = true }, { preferences.acceptPrivacy(); model.acceptPrivacy() }, model::declinePrivacy)
            CustomerRoute.Home -> {
                val data = state.data
                when {
                    state.switchingPlan || (data == null && state.busy) -> CustomerLoading(tab, { tab = it })
                    data == null -> AccountLoadError(state.message, state.busy, model::refresh, model::logout)
                    paymentHelp -> HowToPay(data) { paymentHelp = false }
                    else -> Dashboard(data, tab, { tab = it }, { paymentHelp = true },
                        plans = data.contracts, onSelectPhone = model::selectPlan,
                        refreshing = state.refreshing, busy = state.busy, onRefresh = model::refresh,
                        phone = data.phone.removePrefix("0"), preferences = preferences,
                        onLanguage = { urdu = !urdu; preferences.setUrdu(urdu) }, onLogout = model::logout,
                        notificationStatus = state.notificationStatus, onReadNotifications = model::markNotificationsRead, onOpenNotification = model::openNotification,
                        onMore = model::morePayments, message = state.message, onDeleteAccount = model::deleteAccount)
                }
            }
        }
    }
}
}

@Composable
private fun AuthError(message: String?) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
}
