package pk.qist.customer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val PaidGreen = Color(0xFF08674D)
private val PaidBackground = Color(0xFFD9F4EB)
private val Divider = Color(0xFFE4E9F4)

@Composable
private fun money(amount: Int): String {
    val formatted = NumberFormat.getIntegerInstance(Locale.US).format(amount)
    return copy("Rs. $formatted", "روپے $formatted")
}

@Composable
internal fun PaymentHistory(data: DashboardSample, onReceipt: (PaymentSample) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.weight(1f)) {
            Caption(copy("Total paid", "کل ادا شدہ"))
            Text(money(data.advance + data.paid * data.installment), color = PaidGreen, style = MaterialTheme.typography.titleLarge)
        }
        Panel(Modifier.weight(1f)) {
            Caption(copy("Remaining", "باقی"))
            Text(money((data.total - data.paid) * data.installment), style = MaterialTheme.typography.titleLarge)
        }
    }
    Panel {
        Text(copy("History", "ادائیگیوں کی تفصیل"), style = MaterialTheme.typography.titleLarge)
        data.payments.forEachIndexed { index, payment ->
            if (index > 0) HorizontalDivider(color = Divider)
            DetailRow(
                title = copy("Installment ${payment.number}", "قسط ${payment.number}"),
                subtitle = "${payment.date} · ${copy(payment.method, "نقد")}",
                value = copy("Rs. ${payment.amount}", "روپے ${payment.amount}"),
                icon = R.drawable.detail_check, tint = PaidGreen, background = PaidBackground,
                valueColor = PaidGreen, onClick = { onReceipt(payment) },
            )
        }
    }
}

@Composable
internal fun PlanDetails(data: DashboardSample) {
    Panel {
        DetailRow(data.model, data.retailer, "", R.drawable.phone_outline)
        Fact(copy("Total price", "کل قیمت"), money(data.advance + data.total * data.installment))
        Fact(copy("Advance paid", "پیشگی ادائیگی"), money(data.advance))
        Fact(copy("Installment", "قسط"), money(data.installment))
        Fact(copy("Plan length", "مدت"), copy("${data.total} months", "${data.total} ماہ"))
    }
    Panel {
        Text(copy("Schedule", "اقساط کا شیڈول"), style = MaterialTheme.typography.titleLarge)
        data.schedule.forEachIndexed { index, date ->
            val paid = index < data.paid
            val due = index == data.paid
            val tint = when {
                paid -> PaidGreen
                due -> Color(0xFF795000)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            if (index > 0) HorizontalDivider(color = Divider)
            DetailRow(
                copy("Installment ${index + 1}", "قسط ${index + 1}"), date, money(data.installment),
                icon = when { paid -> R.drawable.detail_check; due -> R.drawable.detail_clock; else -> R.drawable.tab_plan },
                tint = tint,
                background = when { paid -> PaidBackground; due -> Color(0xFFFFF1D5); else -> Divider },
                status = when { paid -> copy("Paid", "ادا شدہ"); due -> copy("Due soon", "جلد واجب"); else -> copy("Upcoming", "آنے والی") },
            )
        }
    }
}

@Composable
internal fun CustomerProfile(data: DashboardSample, phone: String, onLanguage: () -> Unit, onLogout: () -> Unit, notifications: Boolean, onNotifications: (Boolean) -> Unit, onOpenPage: (String) -> Unit) {
    var dialog by rememberSaveable { mutableStateOf("") }
    val language = copy("English", "اردو")
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(Navy, CircleShape), contentAlignment = Alignment.Center) {
                Text(data.name.take(1), color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(data.name, style = MaterialTheme.typography.titleLarge)
                Caption("+92 $phone")
            }
        }
    }
    Panel {
        Caption(copy("Your retailer", "آپ کا دکاندار"))
        Text(data.retailer, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { dialog = "contact" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(copy("Call", "کال کریں"))
            }
            OutlinedButton(onClick = { dialog = "contact" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(copy("WhatsApp", "واٹس ایپ"), color = Navy)
            }
        }
    }
    Panel {
        SettingsRow(copy("Language", "زبان"), R.drawable.detail_language, onClick = { dialog = "language" }) {
            Caption(language)
            Chevron()
        }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Notifications", "اطلاعات"), R.drawable.detail_bell) {
            Switch(checked = notifications, onCheckedChange = onNotifications)
        }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Help and support", "مدد اور معاونت"), R.drawable.detail_help, onClick = { onOpenPage("help") }) { Chevron() }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Privacy policy", "رازداری کی پالیسی"), R.drawable.detail_shield, onClick = { onOpenPage("privacy") }) { Chevron() }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Log out", "لاگ آؤٹ"), R.drawable.detail_logout, destructive = true, onClick = { dialog = "logout" }) { Chevron() }
    }
    when (dialog) {
        "contact" -> DetailDialog(copy("Contact retailer", "دکاندار سے رابطہ"), copy("This demo has no retailer phone number.", "ڈیمو میں دکاندار کا نمبر موجود نہیں۔")) { dialog = "" }
        "language" -> LanguagePicker(
            selectedUrdu = language == "اردو",
            onDismiss = { dialog = "" },
            onApply = { selected ->
                dialog = ""
                if (selected != (language == "اردو")) onLanguage()
            },
        )
        "logout" -> LogoutConfirmation({ dialog = "" }) {
            dialog = ""
            onLogout()
        }
    }
}

@Composable
private fun SettingsRow(title: String, icon: Int, destructive: Boolean = false, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    val color = if (destructive) Color(0xFFAA381D) else Blue
    val background = if (destructive) Color(0xFFFFE8E0) else Blue.copy(alpha = 0.08f)
    Row(
        Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Badge(icon, color, background)
        Text(title, Modifier.weight(1f), color = if (destructive) color else Navy, style = MaterialTheme.typography.titleMedium)
        trailing()
    }
}

@Composable
private fun DetailRow(
    title: String, subtitle: String, value: String, icon: Int,
    tint: Color = Blue, background: Color = Blue.copy(alpha = 0.08f),
    valueColor: Color = Navy, status: String? = null, onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Badge(icon, tint, background)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Caption(subtitle)
        }
        if (value.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End) {
                Text(value, color = valueColor, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                if (status != null) Text(status, color = tint, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (onClick != null) Chevron()
    }
}

@Composable
private fun Fact(label: String, value: String) {
    HorizontalDivider(color = Divider)
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Badge(icon: Int, tint: Color, background: Color) {
    Box(Modifier.size(36.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(19.dp), tint = tint)
    }
}

@Composable
private fun Chevron() {
    Text(copy("›", "‹"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun DetailDialog(title: String, message: String, close: () -> Unit) {
    AlertDialog(
        onDismissRequest = close,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = close) { Text(copy("Close", "بند کریں")) } },
    )
}
