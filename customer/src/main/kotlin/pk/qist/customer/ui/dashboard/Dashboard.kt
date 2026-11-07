package pk.qist.customer.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import pk.qist.customer.R
import pk.qist.customer.data.CustomerPreferences
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.data.PlanChoice
import pk.qist.customer.ui.CustomerTab
import pk.qist.customer.ui.phones.PhonePicker
import pk.qist.customer.ui.phones.PlanStatusBadge
import pk.qist.customer.ui.components.Chevron
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.notifications.NotificationInbox
import pk.qist.customer.ui.notifications.NotificationPrompt
import pk.qist.customer.ui.payments.CustomerReceipt
import pk.qist.customer.ui.payments.PaymentHistory
import pk.qist.customer.ui.plan.PlanDetails
import pk.qist.customer.ui.profile.CustomerProfile
import pk.qist.customer.ui.profile.ProfileInformation
import pk.qist.customer.ui.theme.*

@Composable
internal fun Dashboard(
    data: DashboardSample,
    tab: CustomerTab,
    onTab: (CustomerTab) -> Unit,
    onHelp: () -> Unit,
    plans: List<PlanChoice>,
    onSelectPhone: (String) -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onMore: () -> Unit,
    busy: Boolean,
    message: String?,
    phone: String,
    preferences: CustomerPreferences,
    onLanguage: () -> Unit,
    onLogout: () -> Unit,
) {
    var phonePicker by rememberSaveable { mutableStateOf(false) }
    if (phonePicker) PhonePicker(plans, data.id.orEmpty(), { onSelectPhone(it); phonePicker = false }, { phonePicker = false })
    var allowed by remember { mutableStateOf(preferences.notificationsEnabled) }
    var showPrompt by rememberSaveable { mutableStateOf(false) }
    var inbox by rememberSaveable { mutableStateOf(false) }
    var unread by rememberSaveable { mutableIntStateOf(0) }

    fun setNotifications(enabled: Boolean) {
        allowed = enabled
        preferences.setNotifications(enabled)
        showPrompt = false
    }

    var profilePage by rememberSaveable { mutableStateOf("") }
    BackHandler(profilePage.isNotEmpty()) { profilePage = "" }
    BackHandler(inbox) { inbox = false }
    if (profilePage.isNotEmpty()) {
        ProfileInformation(data, privacy = profilePage == "privacy") { profilePage = "" }
        return
    }
    if (inbox) {
        NotificationInbox(data, unread, { inbox = false }, { unread = 0 }) { bit ->
            unread = unread and bit.inv()
        }
        return
    }
    if (showPrompt) NotificationPrompt({ setNotifications(true) }, { setNotifications(false) })

    var receiptNumber by rememberSaveable(data.id) { mutableStateOf("") }
    val receipt = data.payments.firstOrNull { it.id == receiptNumber }
    BackHandler(receipt != null) { receiptNumber = "" }
    if (receipt != null) {
        CustomerReceipt(data, receipt) { receiptNumber = "" }
        return
    }

    CustomerScaffold(tab, onTab) { padding ->
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            val scroll = key(tab, data.id) { rememberScrollState() }
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                if (message != null) Surface(color = DueBackground, shape = QistShapes.Input, modifier = Modifier.padding(QistSpacing.Section)) {
                    Text(message, Modifier.padding(QistSpacing.Item), color = Navy)
                }
                if (tab == CustomerTab.Home) {
                    Box {
                        Box(Modifier.fillMaxWidth().height(QistSize.HomeBackdrop).background(Navy))
                        Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                            Row(Modifier.fillMaxWidth().padding(vertical = QistSpacing.Small), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(copy("Assalam o Alaikum", "السلام علیکم"), color = Muted)
                                    Text(data.name, color = CardSurface, style = MaterialTheme.typography.titleLarge)
                                }
                                HomeNotificationsButton(unread != 0) { inbox = true }
                            }
                            if (data.id != null) InstallmentCard(data, onHelp)
                            else Panel { Text(copy("No plans available. Contact your retailer.", "کوئی منصوبہ دستیاب نہیں۔ دکاندار سے رابطہ کریں۔")) }
                            if (data.id != null) Panel(Modifier.clickable(onClickLabel = copy("Switch phone", "فون بدلیں")) { phonePicker = true }) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                                    Box(Modifier.size(QistSize.CircleLarge).background(BlueBackground, QistShapes.Pill), contentAlignment = Alignment.Center) {
                                        Icon(painterResource(R.drawable.phone_outline), contentDescription = null, modifier = Modifier.size(QistSize.IconMax), tint = Blue)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(data.model, style = MaterialTheme.typography.titleMedium)
                                        Text(copy("${plans.size} phones · Tap to switch", "${plans.size} فون · بدلنے کے لیے ٹیپ کریں"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    PlanStatusBadge(data.status, data.days.takeIf { data.dueDate != "—" })
                                    Chevron()
                                }
                            }
                            PaymentsCard(data.payments) { onTab(CustomerTab.Payments) }
                        }
                    }
                } else {
                    Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                        Text(if (tab == CustomerTab.Plan) copy("My plan", "میرا منصوبہ") else tabTitle(tab), style = MaterialTheme.typography.headlineMedium)
                        when (tab) {
                            CustomerTab.Payments -> PaymentHistory(data, busy, onMore) { receiptNumber = it.id }
                            CustomerTab.Plan -> if (data.id == null) Panel { Text(copy("No plan available.", "کوئی منصوبہ دستیاب نہیں۔")) } else PlanDetails(data) { phonePicker = true }
                            CustomerTab.Profile -> CustomerProfile(data, phone, onLanguage, onLogout, allowed, ::setNotifications) { profilePage = it }
                            CustomerTab.Home -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentCard(data: DashboardSample, onHelp: () -> Unit) {
    Panel {
        Text(copy("Next installment", "اگلی قسط"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(QistSpacing.Small))
        Text(copy("Rs. ${data.amount}", "روپے ${data.amount}"), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(QistSpacing.Small))
        Text(when {
            data.status == "cancelled" -> copy("Plan cancelled", "منصوبہ منسوخ ہو گیا")
            data.remaining == 0L -> copy("Nothing due", "کوئی رقم واجب نہیں")
            data.days < 0 -> copy("Due ${data.dueDate} · ${-data.days} days overdue", "${data.dueDate} تک واجب · ${-data.days} دن تاخیر")
            else -> copy("Due ${data.dueDate} · in ${data.days} days", "${data.dueDate} تک · ${data.days} دن باقی")
        })
        Spacer(Modifier.height(QistSpacing.Section))
        LinearProgressIndicator(
            progress = { if (data.total == 0) 0f else (data.paid.toFloat() / data.total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(QistSize.Progress),
            color = Blue,
            trackColor = Divider,
        )
        Spacer(Modifier.height(QistSpacing.Compact))
        Text(copy("${data.paid} of ${data.total} installments paid", "${data.total} میں سے ${data.paid} اقساط ادا ہوئیں"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(QistSpacing.Section))
        Pill(copy("How to pay", "ادائیگی کیسے کریں"), onHelp)
    }
}

@Composable
private fun PaymentsCard(payments: List<PaymentSample>, onViewAll: (() -> Unit)? = null) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(copy("Recent payments", "حالیہ ادائیگیاں"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (onViewAll != null) TextButton(onClick = onViewAll) { Text(copy("View all", "تمام دیکھیں")) }
        }
        payments.take(2).forEach { payment ->
            HorizontalDivider(color = Divider)
            Row(Modifier.fillMaxWidth().padding(vertical = QistSpacing.Item), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(copy(payment.label, payment.label), style = MaterialTheme.typography.titleMedium)
                    Text(copy("Paid ${payment.date}", "ادا شدہ ${payment.date}"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(copy("Rs. ${payment.amount}", "روپے ${payment.amount}"), color = PaidGreen, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun HomeNotificationsButton(unread: Boolean, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(QistSize.TouchTarget),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = DeepSurface, contentColor = CardSurface),
    ) {
        Box(Modifier.size(QistSize.IconMax), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.detail_bell), contentDescription = copy("Notifications", "اطلاعات"), modifier = Modifier.size(QistSize.IconMax))
            if (unread) {
                Box(Modifier.align(Alignment.TopEnd).size(QistSize.UnreadDot).background(Cyan, QistShapes.UnreadBadge))
            }
        }
    }
}
