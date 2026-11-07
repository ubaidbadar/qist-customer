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
import pk.qist.customer.BuildConfig
import pk.qist.customer.data.*
import pk.qist.customer.ui.auth.*
import pk.qist.customer.ui.dashboard.*
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.payments.HowToPay
import pk.qist.customer.ui.profile.PrivacyPolicy

internal enum class CustomerRoute { Welcome, Phone, Password, Verify, SetPassword, Consent, Home }
internal enum class CustomerTab { Home, Payments, Plan, Profile }

@Composable
internal fun CustomerShell() {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { CustomerPreferences(context) }
    val model = viewModel { CustomerViewModel(HttpCustomerRepository(CustomerSessionStore(context), BuildConfig.CUSTOMER_API_URL), preferences.hasAcceptedPrivacy) }
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(CustomerTab.Home) }
    var urdu by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var paymentHelp by rememberSaveable { mutableStateOf(false) }
    var consentPolicy by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.route) {
        password = ""; code = ""
        if (state.route == CustomerRoute.Welcome) { tab = CustomerTab.Home; paymentHelp = false; phone = "" }
    }
    BackHandler(state.route in listOf(CustomerRoute.Phone, CustomerRoute.Password, CustomerRoute.Verify, CustomerRoute.SetPassword), onBack = model::back)
    BackHandler(state.route == CustomerRoute.Home && tab != CustomerTab.Home) { tab = CustomerTab.Home }
    BackHandler(state.route == CustomerRoute.Consent) { if (consentPolicy) consentPolicy = false else model.declinePrivacy() }
    BackHandler(paymentHelp) { paymentHelp = false }
    CompositionLocalProvider(LocalUrdu provides urdu, LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        when (state.route) {
            CustomerRoute.Welcome -> Welcome({ urdu = !urdu }, model::begin)
            CustomerRoute.Phone -> AuthScreen(
                copy("Enter your phone number", "اپنا فون نمبر درج کریں"),
                copy("Use the number registered with your retailer.", "اپنے دکاندار کے پاس رجسٹرڈ نمبر استعمال کریں۔"),
                copy("Continue", "جاری رکھیں"), model::back, { model.start(phone) },
                footer = copy("Not registered? Contact your retailer.", "رجسٹرڈ نہیں؟ دکاندار سے رابطہ کریں۔"), busy = state.busy,
            ) {
                PhoneField(phone, false) { if (!state.busy) phone = normalizePhone(it) }
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
                    PasswordField(password) { if (!state.busy) password = it.take(128) }
                    TextButton(onClick = { model.requestCode(true) }, enabled = !state.busy) {
                        Text(if (creating) copy("Verify again", "دوبارہ تصدیق کریں") else copy("Forgot password?", "پاس ورڈ بھول گئے؟"))
                    }
                    AuthError(state.message)
                }
            }
            CustomerRoute.Verify -> AuthScreen(
                copy("Check your WhatsApp", "اپنا واٹس ایپ دیکھیں"),
                copy("Enter the 6-digit code sent to +92 ${state.phone}.", "+92 ${state.phone} پر بھیجا گیا ۶ ہندسوں کا کوڈ درج کریں۔"),
                copy("Verify", "تصدیق کریں"), model::back, { model.verify(code) }, busy = state.busy, enabled = code.length == 6,
            ) {
                CodeField(code) { if (!state.busy) code = it }
                AuthError(state.message)
                Resend(state.deadline, { code = ""; model.requestCode() }, enabled = !state.busy)
            }
            CustomerRoute.Consent -> if (consentPolicy) {
                AuthScreen(copy("Privacy policy", "رازداری کی پالیسی"), "", copy("Back", "واپس"), { consentPolicy = false }, { consentPolicy = false }) { PrivacyPolicy() }
            } else PrivacyConsent({ consentPolicy = true }, { preferences.acceptPrivacy(); model.acceptPrivacy() }, model::declinePrivacy)
            CustomerRoute.Home -> {
                val data = state.data
                when {
                    state.switchingPlan || (data == null && state.busy) -> CustomerLoading(tab, { tab = it })
                    data == null -> CustomerLoadError(tab, { tab = it }, state.message, model::refresh, model::logout)
                    paymentHelp -> HowToPay(data) { paymentHelp = false }
                    else -> Dashboard(data, tab, { tab = it }, { paymentHelp = true },
                        plans = data.contracts, onSelectPhone = model::selectPlan,
                        refreshing = state.refreshing, busy = state.busy, onRefresh = model::refresh,
                        phone = data.phone.removePrefix("0"), preferences = preferences,
                        onLanguage = { urdu = !urdu }, onLogout = model::logout,
                        onMore = model::morePayments, message = state.message)
                }
            }
        }
    }
}

@Composable
private fun AuthError(message: String?) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
}
