package pk.qist.customer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pk.qist.customer.data.*
import pk.qist.customer.ui.auth.Authentication
import pk.qist.customer.ui.auth.PrivacyConsent
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.dashboard.Dashboard
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.profile.ProfileInformation
import pk.qist.customer.ui.theme.*

internal enum class CustomerTab { Home, Payments, Plan, Profile }

@Composable
internal fun CustomerShell() {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { CustomerPreferences(context) }
    val storedPreferences by produceState<CustomerPreferenceSnapshot?>(null, preferences) { value = preferences.load() }
    val factory = remember(context) { viewModelFactory { initializer { CustomerViewModel(HttpCustomerApi(), EncryptedCustomerSession(context)) } } }
    val model: CustomerViewModel = viewModel(factory = factory)
    val initialPreferences = storedPreferences
    if (initialPreferences == null) { CustomerStartup(); return }
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(CustomerTab.Home) }
    var urdu by rememberSaveable { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(initialPreferences.accepted) }
    var policy by rememberSaveable { mutableStateOf(false) }
    BackHandler(policy) { policy = false }
    CompositionLocalProvider(LocalUrdu provides urdu, LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        if (policy) { ProfileInformation("", "", privacy = true) { policy = false }; return@CompositionLocalProvider }
        if (!accepted && state.content == CustomerContent.SignedOut && state.authPage != AuthPage.Welcome) {
            BackHandler { model.openAuth(AuthPage.Welcome) }
            PrivacyConsent({ policy = true }, { preferences.acceptPrivacy(); accepted = true }, { model.openAuth(AuthPage.Welcome) })
            return@CompositionLocalProvider
        }
        when (val content = state.content) {
            CustomerContent.RestoringSession -> CustomerStartup()
            CustomerContent.Loading -> CustomerSkeleton()
            CustomerContent.SignedOut -> Authentication(state, model) { urdu = !urdu }
            is CustomerContent.Ready -> Dashboard(content.overview, preferences, state.loadingMore, state.error, state.busy,
                tab = tab, onTab = { tab = it }, onSelectPlan = model::refresh, onRefresh = { model.refresh() }, onLoadMore = model::loadMorePayments,
                onLanguage = { urdu = !urdu }, onLogout = { tab = CustomerTab.Home; model.logout() })
            is CustomerContent.Failed -> Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                Text(copy("Could not load your account", "اکاؤنٹ لوڈ نہیں ہو سکا"), style = MaterialTheme.typography.headlineMedium)
                Caption(content.message)
                Pill(copy("Try again", "دوبارہ کوشش کریں"), { model.refresh() })
                TextButton(onClick = model::logout, enabled = !state.busy) { Text(copy("Sign out", "سائن آؤٹ")) }
            }
        }
    }
}
