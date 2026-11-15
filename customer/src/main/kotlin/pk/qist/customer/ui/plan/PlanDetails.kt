package pk.qist.customer.ui.plan

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.testTag
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
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.frequencyLabel
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
            }) { installment -> ScheduleRow(installment, cancelled = data.status == "cancelled") }
        }
    }
}

/**
 * The plan summary uses the exact same structural layout while loading and once
 * account data is available. Only its contents change, so card and fact rows
 * retain their positions.
 */
@Composable
internal fun PlanSummaryFrame(
    header: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Panel(Modifier.testTag("plan-summary-card")) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = header,
        )
        content()
    }
}

@Composable
internal fun PlanFactFrame(
    label: @Composable () -> Unit,
    value: @Composable () -> Unit,
) {
    HorizontalDivider(color = Divider)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp).testTag("plan-fact-row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { label() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { value() }
    }
}

@Composable
private fun PlanSummary(data: DashboardSample, onChange: () -> Unit) {
    PlanSummaryFrame(header = {
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
    }) {
        Fact(copy("Total price", "کل قیمت"), money(data.totalPrice))
        Fact(copy("Advance paid", "پیشگی ادائیگی"), money(data.advance))
        Fact(copy("Installment", "قسط"), money(data.installment))
        val frequency = frequencyLabel(data.frequency, LocalUrdu.current)
        Fact(copy("Plan length", "مدت"), copy("${data.total} installments · $frequency", "${data.total} اقساط · $frequency"))
    }
}

@Composable
private fun ScheduleRow(row: pk.qist.customer.data.Installment, cancelled: Boolean) {
    val paid = !cancelled && (row.status == "paid" || row.status == "waived")
    val overdue = !cancelled && !paid && (row.status == "overdue" || row.dueInDays < 0)
    val dueSoon = !cancelled && !paid && row.dueInDays in 0..3
    val partial = !cancelled && row.status == "partial"
    val tint = when { paid -> PaidGreen; overdue -> pk.qist.customer.ui.theme.Destructive; dueSoon || partial -> DueAmber; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val background = when { paid -> PaidBackground; overdue -> pk.qist.customer.ui.theme.DestructiveBackground; dueSoon || partial -> DueBackground; else -> Divider }
    val label = when {
        cancelled -> copy("Cancelled", "منسوخ")
        row.status == "waived" -> copy("Waived", "معاف")
        paid -> copy("Paid", "ادا شدہ")
        overdue -> copy("Overdue", "تاخیر")
        partial -> copy("Partially paid", "جزوی ادا شدہ")
        dueSoon -> copy("Due soon", "جلد واجب")
        else -> copy("Upcoming", "آئندہ")
    }
    DetailRow(copy("Installment ${row.number}", "قسط ${row.number}"), pk.qist.customer.data.displayDate(row.date), money(row.amount + row.lateFee),
        icon = when { paid -> R.drawable.detail_check; overdue || dueSoon || partial -> R.drawable.detail_clock; else -> R.drawable.tab_plan },
        tint = tint, background = background, status = label)
}

@Composable
private fun Fact(label: String, value: String) {
    PlanFactFrame(
        label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        value = { Text(value, textAlign = androidx.compose.ui.text.style.TextAlign.End,
            style = MaterialTheme.typography.titleMedium) },
    )
}
