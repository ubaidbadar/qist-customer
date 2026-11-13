package pk.qist.customer.ui.plan

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.AccountMessage
import pk.qist.customer.ui.components.panelSection
import pk.qist.customer.ui.theme.QistSpacing
import pk.qist.customer.ui.components.DetailRow
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.BlueBackground
import androidx.compose.ui.res.painterResource
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.money
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.DueAmber
import pk.qist.customer.ui.theme.DueBackground
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen

@Composable
internal fun PlanDetails(data: DashboardSample, message: String?, onChange: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(QistSpacing.Screen)) {
        item(key = "title", contentType = "heading") {
            Text(copy("My plan", "میرا منصوبہ"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(QistSpacing.Section))
            if (message != null) { AccountMessage(message); Spacer(Modifier.height(QistSpacing.Section)) }
        }
        if (data.id == null) item(key = "empty", contentType = "empty") {
            Panel { Text(copy("No plan available.", "کوئی منصوبہ دستیاب نہیں۔")) }
        } else {
            item(key = "summary", contentType = "summary") {
                PlanSummary(data, onChange)
                Spacer(Modifier.height(QistSpacing.Section))
            }
            panelSection("schedule", data.schedule, { it.number.toString() }, title = {
                Text(copy("Schedule", "اقساط کا شیڈول"), style = MaterialTheme.typography.titleLarge)
            }) { installment -> ScheduleRow(installment) }
        }
    }
}

@Composable
private fun PlanSummary(data: DashboardSample, onChange: () -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenBadge(R.drawable.phone_outline, Blue, BlueBackground, 44)
            Column(Modifier.weight(1f)) {
                Text(data.model, style = MaterialTheme.typography.titleMedium)
                Caption(data.retailer)
            }
            TextButton(onClick = onChange, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(copy("Change", "بدلیں"))
                Spacer(Modifier.width(4.dp))
                Icon(painterResource(R.drawable.chevron_forward), null, Modifier.size(18.dp))
            }
        }
        Fact(copy("Total price", "کل قیمت"), money(data.totalPrice))
        Fact(copy("Advance paid", "پیشگی ادائیگی"), money(data.advance))
        Fact(copy("Installment", "قسط"), money(data.installment))
        Fact(copy("Plan length", "مدت"), copy("${data.total} installments · ${data.frequency}", "${data.total} اقساط · ${data.frequency}"))
    }
}

@Composable
private fun ScheduleRow(row: pk.qist.customer.data.Installment) {
    val paid = row.status == "paid" || row.status == "waived"
    val overdue = !paid && (row.status == "overdue" || row.dueInDays < 0)
    val dueSoon = !paid && row.dueInDays in 0..3
    val tint = when { paid -> PaidGreen; overdue -> pk.qist.customer.ui.theme.Destructive; dueSoon || row.status == "partial" -> DueAmber; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val background = when { paid -> PaidBackground; overdue -> pk.qist.customer.ui.theme.DestructiveBackground; dueSoon || row.status == "partial" -> DueBackground; else -> Divider }
    val label = when {
        row.status == "waived" -> copy("Waived", "معاف")
        paid -> copy("Paid", "ادا شدہ")
        overdue -> copy("Overdue", "تاخیر")
        row.status == "partial" -> copy("Partially paid", "جزوی ادا شدہ")
        dueSoon -> copy("Due soon", "جلد واجب")
        else -> copy("Upcoming", "آئندہ")
    }
    DetailRow(copy("Installment ${row.number}", "قسط ${row.number}"), pk.qist.customer.data.displayDate(row.date), money(row.amount + row.lateFee),
        icon = when { paid -> R.drawable.detail_check; overdue || dueSoon || row.status == "partial" -> R.drawable.detail_clock; else -> R.drawable.tab_plan },
        tint = tint, background = background, status = label)
}

@Composable
private fun Fact(label: String, value: String) {
    HorizontalDivider(color = Divider)
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, style = MaterialTheme.typography.titleMedium)
    }
}
