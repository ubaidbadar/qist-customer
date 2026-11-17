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

internal enum class CustomerRoute { Welcome, Methods, Phone, Password, Verify, SetPassword, QuickSignin, VerifyDevice, Consent, Home, SessionExpired }
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
            privacyAccepted = { preferences.hasAcceptedPrivacy })
    }
    val state by model.state.collectAsStateWithLifecycle()
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
    BackHandler(state.serviceGate == null && state.route in listOf(CustomerRoute.Phone, CustomerRoute.Password, CustomerRoute.Verify, CustomerRoute.SetPassword, CustomerRoute.QuickSignin, CustomerRoute.VerifyDevice), onBack = model::back)
    BackHandler(state.serviceGate == null && state.route == CustomerRoute.Home && tab != CustomerTab.Home) { tab = CustomerTab.Home }
    BackHandler(state.serviceGate == null && state.route == CustomerRoute.Consent) { if (consentPolicy) consentPolicy = false else model.declinePrivacy() }
    BackHandler(state.serviceGate == null && paymentHelp) { paymentHelp = false }
    CompositionLocalProvider(LocalUrdu provides urdu, LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        if (state.lockerOffer && state.serviceGate == null) CustomerDialog(
            title = copy("Sign in with this phone?", "اسی فون سے لاگ ان کریں؟"),
            message = copy("Use Qist Locker to verify +92 ${state.phone}. Continue only if this is the number linked to your financed phone.", "قسط والے فون کے لاکر سے +92 ${state.phone} کی تصدیق کریں۔ صرف اپنے رجسٹرڈ نمبر کے لیے جاری رکھیں۔"),
            confirmLabel = copy("Verify and continue", "تصدیق کریں اور جاری رکھیں"), dismissLabel = copy("Use another method", "دوسرا طریقہ استعمال کریں"),
            onConfirm = model::confirmLockerSignin, onDismiss = model::dismissLockerOffer, icon = pk.qist.customer.R.drawable.phone_outline,
        )
        pk.qist.customer.ui.layout.CustomerViewport(authentication = state.route != CustomerRoute.Home) {
            CustomerUpdateHost(enabled = state.route == CustomerRoute.Home && state.serviceGate == null) {
                val gate = state.serviceGate
                if (state.restoring) {
                    SessionRestoreLoading()
                } else if (gate != null) {
                    ServiceAvailabilityScreen(state.checkingService, state.message) { model.checkServiceStatus(true) }
                } else when (state.route) {
                    CustomerRoute.SessionExpired -> SessionExpired(model::signInAgain)
                    CustomerRoute.Welcome -> SigninMethodsScreen(state.message, model::begin) { urdu = !urdu; preferences.setUrdu(urdu) }
                    CustomerRoute.Phone -> PhoneSigninScreen(state, model)
                    CustomerRoute.Methods -> SigninMethodsScreen(state.message, model::begin) { urdu = !urdu; preferences.setUrdu(urdu) }
                    CustomerRoute.QuickSignin -> PhoneVerificationProgress(state.verifiedName, state.signinStage, model::back, model::useCodeInstead)
                    CustomerRoute.VerifyDevice -> AuthScreen(
                        copy("Create your password", "اپنا پاس ورڈ بنائیں"),
                        copy("First verify +92 ${state.phone} using your financed phone. Then choose your password.", "پہلے اپنے قسط والے فون سے +92 ${state.phone} کی تصدیق کریں، پھر پاس ورڈ بنائیں۔"),
                        copy("Get code from Locker", "لاکر سے کوڈ لیں"), model::back, { model.requestApproval(state.phone) }, busy = state.busy,
                    ) {
                        TextButton(onClick = model::askLockerSignin, enabled = !state.busy) { Text(copy("Use Locker on this phone", "اسی فون کے لاکر سے تصدیق کریں")) }
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
