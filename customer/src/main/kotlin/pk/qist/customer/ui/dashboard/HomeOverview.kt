package pk.qist.customer.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.paymentLabel
import pk.qist.customer.ui.phones.PlanStatusBadge
import pk.qist.customer.ui.theme.*

/**
 * The loading and loaded Home states share the same backdrop, horizontal insets,
 * header row and card spacing so the first installment card does not jump.
 */
@Composable
internal fun HomeFrame(
    headerContent: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(160.dp).background(Navy).testTag("home-navy-header"))
        Column(Modifier.fillMaxWidth().padding(QistSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, content = headerContent)
            content()
        }
    }
}

@Composable
internal fun HomeOverview(data: DashboardSample, message: String?, unread: Boolean, onHelp: () -> Unit,
    onPayments: () -> Unit, onPhones: () -> Unit, onInbox: () -> Unit, onContact: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (message != null) Box(Modifier.padding(QistSpacing.Screen)) { AccountMessage(message) }
        HomeFrame(headerContent = {
            Column(Modifier.weight(1f)) {
                Text(copy("Assalam o Alaikum", "السلام علیکم"), color = Muted)
                Text(data.name, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            HomeNotificationsButton(unread, onInbox)
        }) {
            if (data.lockState == "locked") PhoneLockNotice(data, data.fromCache)
            if (data.id != null) InstallmentCard(data, onHelp, onPayments)
            else Panel {
                AccountEmptyState(R.drawable.phone_outline, copy("No phone added yet", "ابھی کوئی فون شامل نہیں"),
                    copy("Your retailer has not added a phone to your account yet. Contact them to get started.", "دکاندار نے ابھی آپ کے اکاؤنٹ میں فون شامل نہیں کیا۔ شروع کرنے کے لیے ان سے رابطہ کریں۔")) {
                    Spacer(Modifier.height(24.dp))
                    Pill(copy("Contact your retailer", "اپنے دکاندار سے رابطہ کریں"), onContact)
                }
            }
            if (data.id != null) Panel(Modifier.clickable(onClickLabel = copy("Switch phone", "فون بدلیں"), onClick = onPhones)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                    ScreenBadge(R.drawable.phone_outline, Blue, Blue.copy(alpha = 0.08f), 44)
                    Column(Modifier.weight(1f)) {
                        Text(data.model, style = MaterialTheme.typography.titleMedium)
                        Text(copy("${data.contracts.size} phones · Tap to switch", "${data.contracts.size} فون · بدلنے کے لیے ٹیپ کریں"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    PlanStatusBadge(data.status, data.days.takeIf { data.dueDate != "—" }, locked = data.lockState == "locked")
                    Chevron()
                }
            }
            if (data.id != null) PaymentsCard(data.payments, onPayments)
        }
    }
}

/**
 * A single vertical rhythm for the first Home card in loading and loaded states.
 * Text and skeleton placeholders only supply content, never their own spacing.
 */
@Composable
internal fun HomeInstallmentFrame(
    label: @Composable () -> Unit,
    amount: @Composable () -> Unit,
    description: @Composable () -> Unit,
    progress: @Composable () -> Unit,
    count: @Composable () -> Unit,
    action: @Composable () -> Unit,
) {
    Panel(Modifier.testTag("home-first-card")) {
        label()
        Spacer(Modifier.height(8.dp))
        amount()
        Spacer(Modifier.height(8.dp))
        description()
        Spacer(Modifier.height(16.dp))
        progress()
        Spacer(Modifier.height(6.dp))
        count()
        Spacer(Modifier.height(16.dp))
        action()
    }
}

@Composable
private fun InstallmentCard(data: DashboardSample, onHelp: () -> Unit, onReceipts: () -> Unit) {
    val cancelled = data.status == "cancelled"
    val completed = data.completed && data.remaining == 0L
    val overdue = !cancelled && !completed && data.remaining > 0L && data.days < 0
    val showReceipts = completed || cancelled
    HomeInstallmentFrame(
        label = {
            Text(when {
                cancelled -> copy("Plan status", "منصوبے کی حالت")
                completed -> copy("Plan completed", "منصوبہ مکمل ہو گیا")
                overdue -> copy("Overdue installment", "تاخیر شدہ قسط")
                else -> copy("Next installment", "اگلی قسط")
            }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        },
        amount = {
            Text(when {
                cancelled -> copy("Cancelled", "منسوخ")
                completed -> copy("All paid", "سب ادا ہو گیا")
                else -> copy("Rs. ${data.amount}", "روپے ${data.amount}")
            }, style = MaterialTheme.typography.headlineLarge)
        },
        description = {
            Text(when {
                completed -> copy("You have paid every installment. Thank you!", "آپ نے تمام اقساط ادا کر دی ہیں۔ شکریہ!")
                cancelled -> copy("Contact your retailer for details about this plan.", "اس منصوبے کی تفصیل کے لیے اپنے دکاندار سے رابطہ کریں۔")
                data.remaining == 0L -> copy("Nothing due", "کوئی رقم واجب نہیں")
                data.days < 0 -> copy("Overdue by ${-data.days} days · was due ${data.dueDate}", "${-data.days} دن تاخیر · آخری تاریخ ${data.dueDate}")
                else -> copy("Due ${data.dueDate} · in ${data.days} days", "${data.dueDate} تک · ${data.days} دن باقی")
            }, color = if (overdue) pk.qist.customer.ui.theme.Destructive else Navy)
        },
        progress = {
            LinearProgressIndicator(
                progress = { if (completed) 1f else if (data.total == 0) 0f else (data.paid.toFloat() / data.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Blue,
                trackColor = Divider,
            )
        },
        count = {
            Text(copy("${data.paid} of ${data.total} installments paid", "${data.total} میں سے ${data.paid} اقساط ادا ہوئیں"),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        action = {
            Pill(if (showReceipts) copy("View receipts", "رسیدیں دیکھیں") else copy("How to pay", "ادائیگی کیسے کریں"),
                if (showReceipts) onReceipts else onHelp)
        },
    )
}

@Composable
private fun PaymentsCard(payments: List<PaymentSample>, onViewAll: (() -> Unit)? = null) {
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(copy("Recent payments", "حالیہ ادائیگیاں"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (onViewAll != null) TextButton(onClick = onViewAll) { Text(copy("View all", "تمام دیکھیں")) }
        }
        if (payments.isEmpty()) Caption(copy("No payments recorded yet.", "ابھی کوئی ادائیگی درج نہیں ہوئی۔"))
        payments.take(2).forEach { payment ->
            HorizontalDivider(color = Divider)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(paymentLabel(payment, LocalUrdu.current), style = MaterialTheme.typography.titleMedium)
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

