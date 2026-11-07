package pk.qist.customer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pk.qist.customer.BuildConfig
import pk.qist.customer.data.CustomerPreferences
import pk.qist.customer.data.CustomerSessionStore
import pk.qist.customer.data.HttpCustomerRepository
import pk.qist.customer.ui.auth.*
import pk.qist.customer.ui.dashboard.Dashboard
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.payments.HowToPay
import pk.qist.customer.ui.profile.DemoPrivacy

internal enum class CustomerRoute { Welcome, Phone, Recovery, Verify, Consent, Home }
internal enum class CustomerTab { Home, Payments, Plan, Profile }

@Composable
internal fun CustomerShell() {
    val appContext = LocalContext.current.applicationContext
    val preferences = remember(appContext) { CustomerPreferences(appContext) }
    val model = viewModel { CustomerViewModel(HttpCustomerRepository(CustomerSessionStore(appContext), BuildConfig.CUSTOMER_API_URL), preferences.hasAcceptedPrivacy) }
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(CustomerTab.Home) }
    var urdu by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable { mutableStateOf("") }
    // Credentials never enter saved instance state.
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var invalidPhone by remember { mutableStateOf(false) }
    var paymentHelp by rememberSaveable { mutableStateOf(false) }
    var consentPolicy by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.route) { password = ""; code = ""; invalidPhone = false }
    LaunchedEffect(state.route == CustomerRoute.Welcome) {
        if (state.route == CustomerRoute.Welcome) { phone = ""; paymentHelp = false; tab = CustomerTab.Home }
    }
    fun back() {
        if (state.busy) return
        model.navigate(if (state.route == CustomerRoute.Verify) CustomerRoute.Recovery else CustomerRoute.Welcome)
    }
    BackHandler(state.route in listOf(CustomerRoute.Phone, CustomerRoute.Recovery, CustomerRoute.Verify), onBack = ::back)
    BackHandler(state.route == CustomerRoute.Home && tab != CustomerTab.Home) { tab = CustomerTab.Home }
    BackHandler(state.route == CustomerRoute.Consent) {
        if (consentPolicy) consentPolicy = false else model.declinePrivacy()
    }
    BackHandler(paymentHelp) { paymentHelp = false }
    CompositionLocalProvider(LocalUrdu provides urdu, LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        when (state.route) {
            CustomerRoute.Welcome -> Welcome({ urdu = !urdu }, { if (!state.restoring) model.navigate(CustomerRoute.Phone) })
            CustomerRoute.Phone, CustomerRoute.Recovery -> {
                val recovery = state.route == CustomerRoute.Recovery
                AuthScreen(
                    title = copy(if (recovery) "Set / reset password" else "Enter your phone number", if (recovery) "پاس ورڈ بنائیں / تبدیل کریں" else "اپنا فون نمبر درج کریں"),
                    subtitle = copy(if (recovery) "We will send a WhatsApp code to your registered number." else "Use the number your retailer registered and your password.", if (recovery) "آپ کے رجسٹرڈ نمبر پر واٹس ایپ کوڈ بھیجا جائے گا۔" else "دکاندار کے رجسٹر کردہ نمبر اور پاس ورڈ سے لاگ ان کریں۔"),
                    action = if (state.busy) copy("Please wait…", "انتظار کریں…") else copy(if (recovery) "Send code" else "Sign in", if (recovery) "کوڈ بھیجیں" else "لاگ ان کریں"),
                    footer = copy("Not registered? Ask your retailer to add you.", "رجسٹرڈ نہیں؟ اپنے دکاندار سے رابطہ کریں۔"),
                    onBack = ::back, enabled = !state.busy && (recovery || password.length in 8..128),
                    onAction = {
                        invalidPhone = !phone.matches(Regex("3[0-9]{9}"))
                        if (!invalidPhone) {
                            if (recovery) model.requestCode(phone) else model.login(phone, password, preferences.hasAcceptedPrivacy)
                        }
                    },
                ) {
                    PhoneField(phone, invalidPhone) { if (!state.busy) { phone = normalizePhone(it); invalidPhone = false } }
                    if (!recovery) {
                        PasswordField(password) { password = it }
                        TextButton(onClick = { model.navigate(CustomerRoute.Recovery) }, enabled = !state.busy) { Text(copy("Set / forgot password", "پاس ورڈ بنائیں / بھول گئے")) }
                    }
                    if (state.resetComplete) Text(copy("Password saved. Sign in with your new password.", "پاس ورڈ محفوظ ہو گیا۔ نئے پاس ورڈ سے لاگ ان کریں۔"))
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            CustomerRoute.Verify -> AuthScreen(
                title = copy("Verify your number", "اپنے نمبر کی تصدیق کریں"),
                subtitle = copy("Enter the WhatsApp code for +92 $phone and choose a password (8–128 characters).", "+92 $phone کا واٹس ایپ کوڈ اور نیا پاس ورڈ (۸ تا ۱۲۸ حروف) درج کریں۔"),
                action = if (state.busy) copy("Please wait…", "انتظار کریں…") else copy("Save password", "پاس ورڈ محفوظ کریں"),
                onBack = ::back, enabled = !state.busy && code.length == 6 && password.length in 8..128,
                onAction = { model.reset(phone, code, password) },
            ) {
                CodeField(code) { code = it }
                PasswordField(password) { password = it }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Resend(state.deadline, { code = ""; model.requestCode(phone) }, !state.busy)
            }
            CustomerRoute.Consent -> if (consentPolicy) {
                AuthScreen(copy("Privacy policy", "رازداری کی پالیسی"), "", copy("Back", "واپس"), { consentPolicy = false }, { consentPolicy = false }) { DemoPrivacy() }
            } else PrivacyConsent(
                onPolicy = { consentPolicy = true },
                onAgree = { preferences.acceptPrivacy(); model.acceptPrivacy() },
                onDecline = model::declinePrivacy,
            )
            CustomerRoute.Home -> {
                val data = state.data
                if (data == null) AuthScreen(
                    copy("Your account", "آپ کا اکاؤنٹ"),
                    state.message ?: copy("Loading your plan…", "آپ کا منصوبہ لوڈ ہو رہا ہے…"),
                    if (state.busy) copy("Please wait…", "انتظار کریں…") else copy("Retry", "دوبارہ کوشش کریں"),
                    { model.logout() }, model::refresh, enabled = !state.busy,
                ) { }
                else if (paymentHelp) HowToPay(data) { paymentHelp = false }
                else Column {
                    if (state.message != null) {
                        Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = model::refresh, enabled = !state.busy) { Text(copy("Retry", "دوبارہ کوشش کریں")) }
                    }
                    Dashboard(data, tab, { tab = it }, { paymentHelp = true }, phone = data.phone.removePrefix("0"), preferences = preferences,
                        onLanguage = { urdu = !urdu }, onLogout = model::logout,
                        busy = state.busy, onRefresh = model::refresh, onPlan = model::selectPlan, onMore = model::morePayments)
                }
            }
        }
    }
}
