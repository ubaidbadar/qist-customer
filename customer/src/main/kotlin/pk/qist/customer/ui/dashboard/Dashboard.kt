package pk.qist.customer.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pk.qist.customer.R
import pk.qist.customer.data.*
import pk.qist.customer.ui.CustomerTab
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.notifications.*
import pk.qist.customer.ui.payments.*
import pk.qist.customer.ui.plan.PlanDetails
import pk.qist.customer.ui.profile.*
import pk.qist.customer.ui.theme.*
import pk.qist.customer.ui.theme.Canvas

@Composable
internal fun Dashboard(data: CustomerOverview, preferences: CustomerPreferences, loadingMore: Boolean, error: String?, busy: Boolean, tab: CustomerTab, onTab: (CustomerTab) -> Unit,
    onSelectPlan: (String) -> Unit, onRefresh: () -> Unit, onLoadMore: () -> Unit, onLanguage: () -> Unit, onLogout: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf<ProfilePage?>(null) }
    var inbox by rememberSaveable { mutableStateOf(false) }
    var help by rememberSaveable { mutableStateOf(false) }
    var receiptId by rememberSaveable { mutableStateOf("") }
    var reminders by remember { mutableStateOf(preferences.notificationsEnabled) }
    var showPrompt by rememberSaveable { mutableStateOf(!preferences.hasNotificationPreference) }
    val plan = data.plan
    val notices = remember(data, reminders) { accountNotices(data, reminders) }
    var readIds by remember(data.phone) { mutableStateOf(preferences.readNotices(data.phone)) }
    val unread = notices.any { it.id !in readIds }
    fun read(ids: Set<String>) { readIds = readIds + ids; preferences.saveReadNotices(data.phone, readIds) }
    fun setReminders(enabled: Boolean) { reminders = enabled; preferences.setNotifications(enabled); showPrompt = false }
    BackHandler(tab != CustomerTab.Home) { onTab(CustomerTab.Home) }
    BackHandler(page != null) { page = null }
    BackHandler(inbox) { inbox = false }
    BackHandler(help) { help = false }
    BackHandler(receiptId.isNotEmpty()) { receiptId = "" }
    val receipt = data.payments.items.firstOrNull { it.id == receiptId }
    if (receipt != null && plan != null) { CustomerReceipt(plan, receipt) { receiptId = "" }; return }
    if (help && plan != null) { HowToPay(plan) { help = false }; return }
    if (page != null) { ProfileInformation(plan?.retailer.orEmpty(), plan?.retailerPhone.orEmpty(), page == ProfilePage.Privacy) { page = null }; return }
    if (inbox) { NotificationInbox(notices, readIds, { inbox = false }, { read(notices.map { it.id }.toSet()) }) { read(setOf(it)) }; return }
    if (showPrompt) NotificationPrompt({ setReminders(true) }, { setReminders(false) })
    val home = tab == CustomerTab.Home
    CustomerSystemBars(home)
    val titles = listOf(copy("Home", "گھر"), copy("Payments", "ادائیگیاں"), copy("My plan", "میرا منصوبہ"), copy("Profile", "پروفائل"))
    val icons = listOf(R.drawable.tab_home, R.drawable.tab_payments, R.drawable.tab_plan, R.drawable.tab_profile)
    Scaffold(containerColor = Canvas,
        topBar = { Spacer(Modifier.fillMaxWidth().background(if (home) Navy else Canvas).statusBarsPadding()) },
        bottomBar = { NavigationBar(containerColor = Color.White) {
            CustomerTab.entries.forEachIndexed { index, item -> NavigationBarItem(tab == item, { onTab(item) },
                icon = { Icon(painterResource(icons[index]), null) }, label = { Text(titles[index], maxLines = 1) }) }
        } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (error != null) Text(error, Modifier.padding(QistSpacing.Screen), color = MaterialTheme.colorScheme.error)
            when (tab) {
                CustomerTab.Home -> HomeOverview(data, unread, { inbox = true }, { help = true }, onSelectPlan, onRefresh) { receiptId = it.id }
                CustomerTab.Payments -> PaymentHistory(data, loadingMore, onLoadMore) { receiptId = it.id }
                CustomerTab.Plan -> PlanDetails(plan)
                CustomerTab.Profile -> Column(Modifier.verticalScroll(rememberScrollState()).padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                    Text(titles[3], style = MaterialTheme.typography.headlineMedium)
                    CustomerProfile(data, onLanguage, onLogout, reminders, ::setReminders, busy) { page = it }
                }
            }
        }
    }
}

@Composable
private fun HomeOverview(data: CustomerOverview, unread: Boolean, onInbox: () -> Unit, onHelp: () -> Unit,
    onSelectPlan: (String) -> Unit, onRefresh: () -> Unit, onReceipt: (CustomerPayment) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(Navy).padding(QistSpacing.Screen)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(copy("Assalam o Alaikum", "السلام علیکم"), color = Muted); Text(data.name, color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                FilledIconButton(onClick = onInbox, colors = IconButtonDefaults.filledIconButtonColors(containerColor = DeepSurface, contentColor = Color.White)) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.detail_bell), copy("Notifications", "اطلاعات"))
                        if (unread) Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(Cyan, QistShapes.UnreadBadge))
                    }
                }
            }
        }
        Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onRefresh) { Text(copy("Refresh", "تازہ کریں")) } }
            if (data.contracts.size > 1) {
                var choosing by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { choosing = true }, modifier = Modifier.fillMaxWidth()) { Text(data.plan?.deviceLabel ?: copy("Choose a plan", "منصوبہ منتخب کریں")) }
                    DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                        data.contracts.forEach { choice -> DropdownMenuItem(text = { Text("${choice.deviceLabel} · ${choice.retailer}") }, onClick = { choosing = false; onSelectPlan(choice.id) }) }
                    }
                }
            }
            val plan = data.plan
            if (plan == null) CustomerEmptyState(copy("Your retailer has not added an installment plan yet.", "آپ کے دکاندار نے ابھی قسطوں کا منصوبہ شامل نہیں کیا۔")) else {
                Panel {
                    val due = plan.nextInstallment
                    Text(if (due == null) copy("No installment outstanding", "کوئی قسط باقی نہیں") else copy("Next installment", "اگلی قسط"), style = MaterialTheme.typography.titleMedium)
                    Text(money(due?.remaining ?: 0L), style = MaterialTheme.typography.headlineLarge)
                    due?.let { Text(copy("Due ${it.dueDate}", "${it.dueDate} تک واجب")) }
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { if (plan.count > 0) (plan.paidCount.toFloat() / plan.count).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth(), color = Blue, trackColor = Divider)
                    Caption(copy("${plan.paidCount} of ${plan.count} installments paid", "${plan.count} میں سے ${plan.paidCount} اقساط ادا ہوئیں"))
                    if (due != null) { Spacer(Modifier.height(16.dp)); Pill(copy("How to pay", "ادائیگی کیسے کریں"), onHelp) }
                }
                Panel {
                    DetailRow(plan.deviceLabel, plan.retailer, "", R.drawable.phone_outline)
                    Text(when (plan.deviceLockState) { "locked" -> copy("Phone locked", "فون لاک ہے"); "unlocked" -> copy("Phone unlocked", "فون ان لاک ہے"); else -> copy("Phone status unavailable", "فون کی حالت دستیاب نہیں") }, style = MaterialTheme.typography.titleMedium)
                    Caption(copy("Last status acknowledged by your phone.", "آپ کے فون کی آخری تصدیق شدہ حالت۔"))
                }
                Panel {
                    Text(copy("Recent payments", "حالیہ ادائیگیاں"), style = MaterialTheme.typography.titleLarge)
                    if (data.payments.items.isEmpty()) Caption(copy("No payments recorded yet.", "ابھی کوئی ادائیگی درج نہیں ہوئی۔"))
                    data.payments.items.take(3).forEach { payment -> PaymentRow(payment) { onReceipt(payment) } }
                }
            }
        }
    }
}
