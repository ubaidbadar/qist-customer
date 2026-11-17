package pk.qist.customer.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.qist.customer.data.*
import pk.qist.customer.ui.CustomerTab
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.notifications.*
import pk.qist.customer.ui.payments.*
import pk.qist.customer.ui.phones.PhonePicker
import pk.qist.customer.ui.plan.PlanDetails
import pk.qist.customer.ui.profile.*
import pk.qist.customer.ui.theme.*

private enum class DashboardPage { Content, Phones, Contact, Inbox, Receipt, About, Terms, Privacy, AboutPrivacy, Help, Documents }

@Composable
internal fun Dashboard(
    data: DashboardSample,
    tab: CustomerTab,
    onTab: (CustomerTab) -> Unit,
    onHelp: () -> Unit,
    onSelectPhone: (String) -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onMore: () -> Unit,
    notificationStatus: String?,
    onReadNotifications: (List<String>) -> Unit,
    onOpenNotification: (String) -> Unit,
    openInbox: Boolean,
    onInboxOpened: () -> Unit,
    busy: Boolean,
    message: String?,
    onDeleteAccount: () -> Unit,
    phone: String,
    preferences: CustomerPreferences,
    onLanguage: () -> Unit,
    onLogout: () -> Unit,
) {
    var page by rememberSaveable(data.id) { mutableStateOf(DashboardPage.Content) }
    var receiptId by rememberSaveable(data.id) { mutableStateOf("") }
    var showPrompt by rememberSaveable { mutableStateOf(!preferences.hasNotificationPreference) }
    LaunchedEffect(openInbox) {
        if (openInbox) { page = DashboardPage.Inbox; showPrompt = false; onInboxOpened() }
    }
    fun back() { page = if (page == DashboardPage.Terms || page == DashboardPage.AboutPrivacy) DashboardPage.About else DashboardPage.Content }
    BackHandler(page !in listOf(DashboardPage.Content, DashboardPage.Phones, DashboardPage.Contact), onBack = ::back)
    when (page) {
        DashboardPage.Documents -> { data.id?.let { pk.qist.customer.documents.DocumentsScreen(it, data.retailer, ::back) }; return }
        DashboardPage.About -> { AboutScreen(busy, message, ::back, { page = DashboardPage.Terms }, { page = DashboardPage.AboutPrivacy }, onDeleteAccount); return }
        DashboardPage.Terms -> { TermsOfService(::back); return }
        DashboardPage.Privacy, DashboardPage.AboutPrivacy, DashboardPage.Help -> {
            ProfileInformation(data, privacy = page != DashboardPage.Help, onBack = ::back); return
        }
        DashboardPage.Inbox -> {
            NotificationInbox(data.notifications, message, busy, ::back,
                { onReadNotifications(data.notifications.filter { !it.read }.map { it.id }) }) { notice ->
                if (!busy) {
                    page = DashboardPage.Content
                    onTab(if (notice.kind.startsWith("payment")) CustomerTab.Payments else CustomerTab.Plan)
                    onOpenNotification(notice.id)
                }
            }
            return
        }
        DashboardPage.Receipt -> {
            // A refreshed first page may omit an older receipt that the customer already opened.
            val openedReceipt = remember(data.id, receiptId) { data.payments.firstOrNull { it.id == receiptId } }
            val receipt = data.payments.firstOrNull { it.id == receiptId } ?: openedReceipt
            if (receipt != null) { CustomerReceipt(data, receipt, ::back); return }
        }
        else -> Unit
    }
    if (page == DashboardPage.Phones) PhonePicker(data.contracts, data.id.orEmpty(), { onSelectPhone(it); back() }, ::back)
    if (page == DashboardPage.Contact) RetailerContactDialog(data.retailerPhone, onDismiss = ::back)
    if (showPrompt && page == DashboardPage.Content) NotificationPrompt({ preferences.setNotifications(true); showPrompt = false }, { preferences.setNotifications(false); showPrompt = false })
    CustomerScaffold(tab, onTab, offline = data.fromCache) { padding ->
        key(tab, data.id) {
            CustomerRefreshBox(refreshing = refreshing, onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(padding)) {
                val visibleMessage = message.takeIf { !data.fromCache }
                when (tab) {
                    CustomerTab.Home -> HomeOverview(data, visibleMessage, data.notifications.any { !it.read }, onHelp,
                        { onTab(CustomerTab.Payments) }, { page = DashboardPage.Phones }, { page = DashboardPage.Inbox }, { page = DashboardPage.Contact })
                    CustomerTab.Payments -> PaymentHistory(data, busy, visibleMessage, onMore) { receiptId = it.id; page = DashboardPage.Receipt }
                    CustomerTab.Plan -> PlanDetails(data, visibleMessage, onDocuments = { page = DashboardPage.Documents }) { page = DashboardPage.Phones }
                    CustomerTab.Profile -> ProfileTab(data, phone, preferences, notificationStatus, visibleMessage, onLanguage, onLogout) { route ->
                        page = when (route) { "about" -> DashboardPage.About; "privacy" -> DashboardPage.Privacy; "help" -> DashboardPage.Help; else -> DashboardPage.Content }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(data: DashboardSample, phone: String, preferences: CustomerPreferences, notificationStatus: String?,
    message: String?, onLanguage: () -> Unit, onLogout: () -> Unit, onPage: (String) -> Unit) {
    val allowed by preferences.notificationPreference.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(QistSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
        Text(copy("Profile", "پروفائل"), style = MaterialTheme.typography.headlineMedium)
        if (message != null) AccountMessage(message)
        CustomerProfile(data.name, phone, onLanguage, onLogout, allowed, preferences::setNotifications, notificationStatus, onPage)
    }
}
