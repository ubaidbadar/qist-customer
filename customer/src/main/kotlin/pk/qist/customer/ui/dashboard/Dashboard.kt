package pk.qist.customer.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
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
import pk.qist.customer.ui.components.AccountEmptyState
import pk.qist.customer.ui.components.RetailerContactDialog
import pk.qist.customer.ui.components.Chevron
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.notifications.NotificationInbox
import pk.qist.customer.ui.notifications.NotificationPrompt
import pk.qist.customer.ui.payments.CustomerReceipt
import pk.qist.customer.ui.payments.EmptyPaymentHistory
import pk.qist.customer.ui.payments.PaymentHistory
import pk.qist.customer.ui.plan.PlanDetails
import pk.qist.customer.ui.profile.CustomerProfile
import pk.qist.customer.ui.profile.AboutScreen
import pk.qist.customer.ui.profile.TermsOfService
import pk.qist.customer.ui.profile.ProfileInformation
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.DueBackground
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.Cyan
import pk.qist.customer.ui.theme.DeepSurface
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.Muted
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

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
    notificationStatus: String?,
    onReadNotifications: (List<String>) -> Unit,
    onOpenNotification: (String) -> Unit,
    busy: Boolean,
    message: String?,
    onDeleteAccount: () -> Unit,
    phone: String,
    preferences: CustomerPreferences,
    onLanguage: () -> Unit,
    onLogout: () -> Unit,
) {
    var retailerContact by rememberSaveable { mutableStateOf(false) }
    if (retailerContact) RetailerContactDialog(data.retailerPhone) { retailerContact = false }
    var phonePicker by rememberSaveable { mutableStateOf(false) }
    if (phonePicker) PhonePicker(plans, data.id.orEmpty(), { onSelectPhone(it); phonePicker = false }, { phonePicker = false })
    val allowed by preferences.notificationPreference.collectAsState()
    var showPrompt by rememberSaveable { mutableStateOf(!preferences.hasNotificationPreference) }
    var inbox by rememberSaveable { mutableStateOf(false) }
    var profilePage by rememberSaveable { mutableStateOf("") }
    var receiptNumber by rememberSaveable(data.id) { mutableStateOf("") }
    val unread = data.notifications.count { !it.read }
    val openInbox by pk.qist.customer.platform.CustomerPushSignals.openInbox.collectAsState()
    LaunchedEffect(openInbox) { if (openInbox) { profilePage = ""; receiptNumber = ""; phonePicker = false; showPrompt = false; inbox = true; pk.qist.customer.platform.CustomerPushSignals.openInbox.value = false } }

    fun setNotifications(enabled: Boolean) {
        preferences.setNotifications(enabled)
        showPrompt = false
    }

    fun backProfile() { profilePage = if (profilePage == "terms" || profilePage == "about-privacy") "about" else "" }
    BackHandler(profilePage.isNotEmpty(), onBack = ::backProfile)
    BackHandler(inbox) { inbox = false }
    if (profilePage.isNotEmpty()) {
        when (profilePage) {
            "about" -> AboutScreen(busy, message, ::backProfile, { profilePage = "terms" }, { profilePage = "about-privacy" }, onDeleteAccount)
            "terms" -> TermsOfService(::backProfile)
            else -> ProfileInformation(data, privacy = profilePage == "privacy" || profilePage == "about-privacy", onBack = ::backProfile)
        }
        return
    }
    if (inbox) {
        NotificationInbox(data.notifications, message, busy, { inbox = false }, { onReadNotifications(data.notifications.filter { !it.read }.map { it.id }) }) { notice ->
            inbox = false
            onTab(if (notice.kind == "payment" || notice.kind == "payment_voided") CustomerTab.Payments else CustomerTab.Plan)
            onOpenNotification(notice.id)
        }
        return
    }
    if (showPrompt) NotificationPrompt({ setNotifications(true) }, { setNotifications(false) })

    val receipt = data.payments.firstOrNull { it.id == receiptNumber }
    BackHandler(receipt != null) { receiptNumber = "" }
    if (receipt != null) {
        CustomerReceipt(data, receipt) { receiptNumber = "" }
        return
    }

    val refreshState = rememberPullToRefreshState()
    CustomerScaffold(tab, onTab, offline = data.fromCache) { padding ->
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, state = refreshState,
            modifier = Modifier.fillMaxSize().padding(padding), indicator = {
                PullToRefreshDefaults.Indicator(state = refreshState, isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter), containerColor = Color.White, color = Blue)
            }) {
            if (tab == CustomerTab.Payments && data.payments.isEmpty()) {
                EmptyPaymentHistory(message.takeIf { !data.fromCache })
            } else {
                val scroll = key(tab, data.id) { rememberScrollState() }
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    if (message != null && !data.fromCache) Surface(color = DueBackground, shape = QistShapes.Input, modifier = Modifier.padding(16.dp)) {
                        Text(message, Modifier.padding(12.dp), color = Navy)
                    }
                    if (tab == CustomerTab.Home) {
                        Box {
                            Box(Modifier.fillMaxWidth().height(160.dp).background(Navy))
                            Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(copy("Assalam o Alaikum", "السلام علیکم"), color = Muted)
                                        Text(data.name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                                    }
                                    HomeNotificationsButton(unread != 0) { inbox = true }
                                }
                                if (data.lockState == "locked") PhoneLockNotice(data, data.fromCache)
                                if (data.id != null) InstallmentCard(data, onHelp) { onTab(CustomerTab.Payments) }
                                else Panel {
                                    AccountEmptyState(R.drawable.phone_outline, copy("No phone added yet", "ابھی کوئی فون شامل نہیں"),
                                        copy("Your retailer has not added a phone to your account yet. Contact them to get started.", "دکاندار نے ابھی آپ کے اکاؤنٹ میں فون شامل نہیں کیا۔ شروع کرنے کے لیے ان سے رابطہ کریں۔")) {
                                        Spacer(Modifier.height(24.dp))
                                        Pill(copy("Contact your retailer", "اپنے دکاندار سے رابطہ کریں"), { retailerContact = true })
                                    }
                                }
                                if (data.id != null) Panel(Modifier.clickable(onClickLabel = copy("Switch phone", "فون بدلیں")) { phonePicker = true }) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                                        Box(Modifier.size(44.dp).background(Blue.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(painterResource(R.drawable.phone_outline), contentDescription = null, tint = Blue)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(data.model, style = MaterialTheme.typography.titleMedium)
                                            Text(copy("${plans.size} phones · Tap to switch", "${plans.size} فون · بدلنے کے لیے ٹیپ کریں"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (data.lockState == "locked") LockedBadge()
                                        else PlanStatusBadge(data.status, data.days.takeIf { data.dueDate != "—" })
                                        Chevron()
                                    }
                                }
                                if (data.id != null) PaymentsCard(data.payments) { onTab(CustomerTab.Payments) }
                            }
                        }
                    } else {
                        Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                            Text(if (tab == CustomerTab.Plan) copy("My plan", "میرا منصوبہ") else tabTitle(tab), style = MaterialTheme.typography.headlineMedium)
                            when (tab) {
                                CustomerTab.Payments -> PaymentHistory(data, busy, onMore) { receiptNumber = it.id }
                                CustomerTab.Plan -> if (data.id == null) Panel { Text(copy("No plan available.", "کوئی منصوبہ دستیاب نہیں۔")) } else PlanDetails(data) { phonePicker = true }
                                CustomerTab.Profile -> CustomerProfile(data, phone, onLanguage, onLogout, allowed, ::setNotifications, notificationStatus) { profilePage = it }
                                CustomerTab.Home -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentCard(data: DashboardSample, onHelp: () -> Unit, onReceipts: () -> Unit) {
    val completed = data.completed && data.remaining == 0L
    val overdue = !completed && data.remaining > 0L && data.days < 0
    Panel {
        Text(when {
            completed -> copy("Plan completed", "منصوبہ مکمل ہو گیا")
            overdue -> copy("Overdue installment", "تاخیر شدہ قسط")
            else -> copy("Next installment", "اگلی قسط")
        }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(if (completed) copy("All paid", "سب ادا ہو گیا") else copy("Rs. ${data.amount}", "روپے ${data.amount}"), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(when {
            completed -> copy("You have paid every installment. Thank you!", "آپ نے تمام اقساط ادا کر دی ہیں۔ شکریہ!")
            data.status == "cancelled" -> copy("Plan cancelled", "منصوبہ منسوخ ہو گیا")
            data.remaining == 0L -> copy("Nothing due", "کوئی رقم واجب نہیں")
            data.days < 0 -> copy("Overdue by ${-data.days} days · was due ${data.dueDate}", "${-data.days} دن تاخیر · آخری تاریخ ${data.dueDate}")
            else -> copy("Due ${data.dueDate} · in ${data.days} days", "${data.dueDate} تک · ${data.days} دن باقی")
        }, color = if (overdue) pk.qist.customer.ui.theme.Destructive else Navy)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { if (completed) 1f else if (data.total == 0) 0f else (data.paid.toFloat() / data.total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Blue,
            trackColor = Divider,
        )
        Spacer(Modifier.height(6.dp))
        Text(copy("${data.paid} of ${data.total} installments paid", "${data.total} میں سے ${data.paid} اقساط ادا ہوئیں"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Pill(if (completed) copy("View receipts", "رسیدیں دیکھیں") else copy("How to pay", "ادائیگی کیسے کریں"), if (completed) onReceipts else onHelp)
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
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = DeepSurface, contentColor = Color.White),
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.detail_bell), contentDescription = copy("Notifications", "اطلاعات"))
            if (unread) {
                Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(Cyan, QistShapes.UnreadBadge))
            }
        }
    }
}

@Composable
private fun PhoneLockNotice(data: DashboardSample, offline: Boolean) {
    Surface(color = pk.qist.customer.ui.theme.DestructiveBackground, shape = QistShapes.Card) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.detail_lock), null, Modifier.size(20.dp), tint = pk.qist.customer.ui.theme.Destructive)
            Text(if (offline) copy("Last saved phone status: Locked. Contact ${data.retailer} about unlocking.", "آخری محفوظ حالت: لاک۔ ان لاک کے لیے ${data.retailer} سے رابطہ کریں۔")
                else copy("Your phone was last reported locked. Contact ${data.retailer} about unlocking.", "آپ کے فون کی آخری حالت لاک ہے۔ ان لاک کے لیے ${data.retailer} سے رابطہ کریں۔"),
                color = pk.qist.customer.ui.theme.Destructive, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LockedBadge() {
    Surface(color = pk.qist.customer.ui.theme.DestructiveBackground, shape = CircleShape) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.detail_lock), null, Modifier.size(13.dp), tint = pk.qist.customer.ui.theme.Destructive)
            Text(copy("Locked", "لاک"), color = pk.qist.customer.ui.theme.Destructive, style = MaterialTheme.typography.labelMedium)
        }
    }
}
