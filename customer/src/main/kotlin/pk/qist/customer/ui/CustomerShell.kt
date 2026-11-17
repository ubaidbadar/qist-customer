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

internal enum class CustomerRoute { Welcome, Phone, Password, Verify, SetPassword, Approval, Consent, Home, SessionExpired }
internal enum class CustomerTab { Home, Payments, Plan, Profile }

@Composable
internal fun CustomerShell() {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { CustomerPreferences(context) }
    val model = viewModel {
        val storage = KeystoreTextStore(context)
        CustomerViewModel(HttpCustomerRepository(CustomerSessionStore(storage), BuildConfig.CUSTOMER_API_URL,
            overviewCache = BoundOverviewCache(storage), appVersionCode = BuildConfig.VERSION_CODE.toLong()),
            preferences.hasAcceptedPrivacy,
            locker = pk.qist.customer.platform.AndroidLockerAuthenticator(context),
            allowDeviceAutoSignin = preferences.allowDeviceAutoSignin,
            setAutoSigninAllowed = preferences::setDeviceAutoSignin,
            privacyAccepted = { preferences.hasAcceptedPrivacy })
    }
    val state by model.state.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, model) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) model.authForeground(true)
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) model.authForeground(false)
        }
        lifecycle.addObserver(observer)
        model.authForeground(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
        onDispose { lifecycle.removeObserver(observer); model.authForeground(false) }
    }
    CustomerPushBinding(state.route == CustomerRoute.Home, preferences, model)
    var tab by rememberSaveable { mutableStateOf(CustomerTab.Home) }
    var urdu by rememberSaveable { mutableStateOf(preferences.urdu) }
    var paymentHelp by rememberSaveable { mutableStateOf(false) }
    var consentPolicy by rememberSaveable { mutableStateOf(false) }
    val openNotificationInbox by pk.qist.customer.platform.CustomerPushSignals.openInbox.collectAsStateWithLifecycle()
    LaunchedEffect(openNotificationInbox, state.route) { if (openNotificationInbox && state.route == CustomerRoute.Home) { paymentHelp = false; tab = CustomerTab.Home } }
    LaunchedEffect(state.route) {
        if (state.route == CustomerRoute.Welcome || state.route == CustomerRoute.SessionExpired) { tab = CustomerTab.Home; paymentHelp = false }
    }
    BackHandler(state.serviceGate == null && state.route in listOf(CustomerRoute.Phone, CustomerRoute.Password, CustomerRoute.Verify, CustomerRoute.SetPassword, CustomerRoute.Approval), onBack = model::back)
    BackHandler(state.serviceGate == null && state.route == CustomerRoute.Home && tab != CustomerTab.Home) { tab = CustomerTab.Home }
    BackHandler(state.serviceGate == null && state.route == CustomerRoute.Consent) { if (consentPolicy) consentPolicy = false else model.declinePrivacy() }
    BackHandler(state.serviceGate == null && paymentHelp) { paymentHelp = false }
    CompositionLocalProvider(LocalUrdu provides urdu, LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        pk.qist.customer.ui.layout.CustomerViewport(authentication = state.route != CustomerRoute.Home) {
            CustomerUpdateHost(enabled = state.route == CustomerRoute.Home && state.serviceGate == null) {
                val gate = state.serviceGate
                if (state.restoring) {
                    SessionRestoreLoading()
                } else if (gate != null) {
                    ServiceAvailabilityScreen(state.checkingService, state.message) { model.checkServiceStatus(true) }
                } else when (state.route) {
                    CustomerRoute.SessionExpired -> SessionExpired(model::signInAgain)
                    CustomerRoute.Welcome -> Welcome({ urdu = !urdu; preferences.setUrdu(urdu) }, model::begin)
                    CustomerRoute.Phone -> PhoneSigninScreen(state, model)
                    CustomerRoute.Approval -> AuthScreen(
                        copy("Approve on your financed phone", "اپنے قسط والے فون پر منظوری دیں"),
                        copy("Open Qist sign-in on that phone. Enter this number there and approve only the request you started.", "اس فون پر Qist sign-in کھولیں۔ یہ نمبر وہاں درج کریں اور صرف اپنی درخواست منظور کریں۔"),
                        copy("Use password instead", "پاس ورڈ استعمال کریں"), model::back, { model.usePassword(state.phone) },
                        busy = state.busy, enabled = !state.busy,
                    ) {
                        Text(state.signinNumber.orEmpty(), style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(if (state.approvalPending) copy("Waiting for your approval…", "آپ کی منظوری کا انتظار ہے…") else copy("This request has ended.", "یہ درخواست ختم ہو گئی ہے۔"))
                        TextButton(onClick = { model.useWhatsApp(state.phone) }, enabled = !state.busy) { Text(copy("Use WhatsApp recovery", "واٹس ایپ سے بحالی")) }
                        AuthError(state.message)
                    }
                    CustomerRoute.Password, CustomerRoute.SetPassword -> PasswordSigninScreen(state, model, preferences.hasAcceptedPrivacy)
                    CustomerRoute.Verify -> CodeSigninScreen(state, model)
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
                                onSelectPhone = model::selectPlan,
                                refreshing = state.refreshing, busy = state.busy, onRefresh = model::refresh,
                                phone = data.phone.removePrefix("0"), preferences = preferences,
                                onLanguage = { urdu = !urdu; preferences.setUrdu(urdu) }, onLogout = model::logout,
                                openInbox = openNotificationInbox, onInboxOpened = { pk.qist.customer.platform.CustomerPushSignals.openInbox.value = false },
                                notificationStatus = state.notificationStatus, onReadNotifications = model::markNotificationsRead, onOpenNotification = model::openNotification,
                                onMore = model::morePayments, message = state.message, onDeleteAccount = model::deleteAccount)
                        }
                    }
                }
            }
        }
    }
}
