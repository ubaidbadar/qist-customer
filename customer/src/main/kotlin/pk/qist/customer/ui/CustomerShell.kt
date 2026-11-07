package pk.qist.customer.ui

import android.os.SystemClock
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import pk.qist.customer.data.CustomerPreferences
import pk.qist.customer.data.CustomerRepository
import pk.qist.customer.data.DemoCustomerRepository
import pk.qist.customer.ui.auth.AuthScreen
import pk.qist.customer.ui.auth.CodeField
import pk.qist.customer.ui.auth.PhoneField
import pk.qist.customer.ui.auth.PrivacyConsent
import pk.qist.customer.ui.auth.Resend
import pk.qist.customer.ui.auth.Welcome
import pk.qist.customer.ui.auth.normalizePhone
import pk.qist.customer.ui.dashboard.Dashboard
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.payments.HowToPay
import pk.qist.customer.ui.profile.ProfileInformation

internal enum class CustomerRoute { Welcome, Phone, Verify, Consent, Home }

internal enum class CustomerTab { Home, Payments, Plan, Profile }

@Composable
internal fun CustomerShell() {
    var route by rememberSaveable { mutableStateOf(CustomerRoute.Welcome) }
    var tab by rememberSaveable { mutableStateOf(CustomerTab.Home) }
    var urdu by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var paymentHelp by rememberSaveable { mutableStateOf(false) }
    val appContext = LocalContext.current.applicationContext
    val preferences = remember(appContext) { CustomerPreferences(appContext) }
    var consentPolicy by rememberSaveable { mutableStateOf(false) }
    val repository: CustomerRepository = DemoCustomerRepository
    var plans by remember { mutableStateOf(repository.phones()) }
    var selectedPlan by rememberSaveable { mutableStateOf(plans.first().id) }
    val selected = plans.firstOrNull { it.id == selectedPlan } ?: plans.first()
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                withFrameNanos { }
                plans = repository.phones()
            } finally { refreshing = false }
        }
    }

    fun back() {
        error = false
        route = if (route == CustomerRoute.Verify) CustomerRoute.Phone else CustomerRoute.Welcome
    }
    fun requestCode() {
        error = false
        code = ""
        deadline = SystemClock.elapsedRealtime() + 30_000
        route = CustomerRoute.Verify
    }

    BackHandler(route == CustomerRoute.Phone || route == CustomerRoute.Verify, onBack = ::back)
    BackHandler(route == CustomerRoute.Home && tab != CustomerTab.Home) { tab = CustomerTab.Home }
    BackHandler(route == CustomerRoute.Consent) {
        if (consentPolicy) consentPolicy = false else {
            phone = ""
            route = CustomerRoute.Welcome
        }
    }
    BackHandler(paymentHelp) { paymentHelp = false }
    CompositionLocalProvider(
        LocalUrdu provides urdu,
        LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        when (route) {
            CustomerRoute.Welcome -> Welcome({ urdu = !urdu }, { route = CustomerRoute.Phone })
            CustomerRoute.Phone -> AuthScreen(
                title = copy("Enter your phone number", "اپنا فون نمبر درج کریں"),
                subtitle = copy("Use the number your retailer registered. Try any valid mobile number in this demo.", "ڈیمو میں کوئی بھی درست موبائل نمبر استعمال کریں۔"),
                action = copy("Send code", "کوڈ بھیجیں"),
                footer = copy("Not registered? Ask your retailer to add you.", "رجسٹرڈ نہیں؟ اپنے دکاندار سے رابطہ کریں۔"),
                onBack = ::back,
                onAction = { if (phone.matches(Regex("3[0-9]{9}"))) requestCode() else error = true },
            ) {
                PhoneField(phone, error) { phone = normalizePhone(it); error = false }
            }
            CustomerRoute.Verify -> AuthScreen(
                title = copy("Verify your number", "اپنے نمبر کی تصدیق کریں"),
                subtitle = copy("Enter the 6-digit code for +92 $phone. Demo code: 123456.", "+92 $phone کے لیے ۶ ہندسوں کا کوڈ درج کریں۔ ڈیمو کوڈ: 123456۔"),
                action = copy("Verify", "تصدیق کریں"),
                onBack = ::back,
                onAction = {
                    if (repository.verify(code)) {
                        tab = CustomerTab.Home
                        code = ""
                        route = if (preferences.hasAcceptedPrivacy) CustomerRoute.Home else CustomerRoute.Consent
                    } else error = true
                },
            ) {
                CodeField(code) { code = it; error = false }
                if (error) Text(copy("Enter the demo code 123456.", "ڈیمو کوڈ 123456 درج کریں۔"), color = MaterialTheme.colorScheme.error)
                Resend(deadline, ::requestCode)
            }
            CustomerRoute.Consent -> if (consentPolicy) {
                ProfileInformation(selected, privacy = true) { consentPolicy = false }
            } else {
                PrivacyConsent(
                    onPolicy = { consentPolicy = true },
                    onAgree = {
                        preferences.acceptPrivacy()
                        route = CustomerRoute.Home
                    },
                    onDecline = {
                        phone = ""
                        route = CustomerRoute.Welcome
                    },
                )
            }
            CustomerRoute.Home -> if (paymentHelp) {
                HowToPay(selected) { paymentHelp = false }
            } else Dashboard(
                selected, tab, { tab = it }, { paymentHelp = true },
                plans = plans,
                onSelectPhone = { selectedPlan = it },
                refreshing = refreshing,
                onRefresh = ::refresh,
                phone = phone,
                preferences = preferences,
                onLanguage = { urdu = !urdu },
                onLogout = {
                    selectedPlan = plans.first().id
                    phone = ""
                    code = ""
                    error = false
                    deadline = 0L
                    paymentHelp = false
                    tab = CustomerTab.Home
                    route = CustomerRoute.Welcome
                },
            )
        }
    }
}
