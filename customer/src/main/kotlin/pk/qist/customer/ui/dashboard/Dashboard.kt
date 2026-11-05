package pk.qist.customer.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.ui.CustomerTab
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
import pk.qist.customer.ui.theme.Blue
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
    phone: String,
    onLanguage: () -> Unit,
    onLogout: () -> Unit,
) {
    val prefs = LocalContext.current.getSharedPreferences("customer_notifications", 0)
    var allowed by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var showPrompt by rememberSaveable { mutableStateOf(!prefs.contains("enabled")) }
    var inbox by rememberSaveable { mutableStateOf(false) }
    var unread by rememberSaveable { mutableIntStateOf(3) }

    fun setNotifications(enabled: Boolean) {
        allowed = enabled
        prefs.edit().putBoolean("enabled", enabled).apply()
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

    val tabs = listOf(copy("Home", "گھر"), copy("Payments", "ادائیگیاں"), copy("Plan", "منصوبہ"), copy("Profile", "پروفائل"))
    val icons = listOf(R.drawable.tab_home, R.drawable.tab_payments, R.drawable.tab_plan, R.drawable.tab_profile)
    var receiptNumber by rememberSaveable { mutableIntStateOf(0) }
    val receipt = data.payments.firstOrNull { it.number == receiptNumber }
    BackHandler(receipt != null) { receiptNumber = 0 }
    if (receipt != null) {
        CustomerReceipt(data, receipt) { receiptNumber = 0 }
        return
    }

    val home = tab == CustomerTab.Home
    val window = LocalActivity.current?.window
    DisposableEffect(window, home) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.isAppearanceLightStatusBars = !home
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            Spacer(Modifier.fillMaxWidth().background(if (home) Navy else Canvas).statusBarsPadding())
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                CustomerTab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { onTab(item) },
                        icon = { Icon(painterResource(icons[index]), contentDescription = null) },
                        label = { Text(tabs[index], maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            if (tab == CustomerTab.Home) {
                Box {
                    Box(Modifier.fillMaxWidth().height(160.dp).background(Navy))
                    Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(copy("Assalam o Alaikum", "السلام علیکم"), color = Muted)
                                Text(data.name, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            }
                            HomeNotificationsButton(unread != 0) { inbox = true }
                        }
                        InstallmentCard(data, onHelp)
                        Panel {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                                Box(Modifier.size(44.dp).background(Blue.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.phone_outline), contentDescription = null, tint = Blue)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(data.model, style = MaterialTheme.typography.titleMedium)
                                    Text(copy("Your phone", "آپ کا فون"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    copy("✓ Active", "✓ فعال"),
                                    Modifier.background(PaidBackground, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = PaidGreen,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        PaymentsCard(data.payments) { onTab(CustomerTab.Payments) }
                    }
                }
            } else {
                Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                    Text(if (tab == CustomerTab.Plan) copy("My plan", "میرا منصوبہ") else tabs[tab.ordinal], style = MaterialTheme.typography.headlineMedium)
                    when (tab) {
                        CustomerTab.Payments -> PaymentHistory(data) { receiptNumber = it.number }
                        CustomerTab.Plan -> PlanDetails(data)
                        CustomerTab.Profile -> CustomerProfile(data, phone, onLanguage, onLogout, allowed, ::setNotifications) { profilePage = it }
                        CustomerTab.Home -> Unit
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
        Spacer(Modifier.height(8.dp))
        Text(copy("Rs. ${data.amount}", "روپے ${data.amount}"), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(copy("Due ${data.dueDate} · in ${data.days} days", "${data.dueDate} تک · ${data.days} دن باقی"))
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (data.paid.toFloat() / data.total).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Blue,
            trackColor = Divider,
        )
        Spacer(Modifier.height(6.dp))
        Text(copy("${data.paid} of ${data.total} installments paid", "${data.total} میں سے ${data.paid} اقساط ادا ہوئیں"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
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
        payments.forEach { payment ->
            HorizontalDivider(color = Divider)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(copy("Installment ${payment.number}", "قسط ${payment.number}"), style = MaterialTheme.typography.titleMedium)
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
