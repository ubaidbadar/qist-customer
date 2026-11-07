package pk.qist.customer.ui.plan

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.DetailRow
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.ScreenBadge
import androidx.compose.ui.res.painterResource
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.money
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun PlanDetails(data: DashboardSample, onChange: () -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth().padding(bottom = QistSpacing.Item), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
            ScreenBadge(R.drawable.phone_outline, Blue, BlueBackground, QistSize.CircleLarge)
            Column(Modifier.weight(1f)) {
                Text(data.model, style = MaterialTheme.typography.titleMedium)
                Caption(data.retailer)
            }
            TextButton(onClick = onChange, contentPadding = PaddingValues(horizontal = QistSpacing.Tiny)) {
                Text(copy("Change", "بدلیں"))
                Spacer(Modifier.width(QistSpacing.Tiny))
                Icon(painterResource(R.drawable.chevron_forward), null, Modifier.size(QistSize.IconMedium))
            }
        }
        Fact(copy("Total price", "کل قیمت"), money(data.totalPrice))
        Fact(copy("Advance paid", "پیشگی ادائیگی"), money(data.advance))
        Fact(copy("Installment", "قسط"), money(data.installment))
        Fact(copy("Plan length", "مدت"), copy("${data.total} installments · ${data.frequency}", "${data.total} اقساط · ${data.frequency}"))
    }
    Panel {
        Text(copy("Schedule", "اقساط کا شیڈول"), style = MaterialTheme.typography.titleLarge)
        data.schedule.forEachIndexed { index, row ->
            val paid = row.status == "paid" || row.status == "waived"
            val due = row.status == "overdue" || row.status == "partial" || (!paid && row.number == data.schedule.firstOrNull { it.status != "paid" && it.status != "waived" }?.number)
            val tint = when {
                paid -> PaidGreen
                due -> DueAmber
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            if (index > 0) HorizontalDivider(color = Divider)
            DetailRow(
                copy("Installment ${row.number}", "قسط ${row.number}"), pk.qist.customer.data.displayDate(row.date), money(row.amount + row.lateFee),
                icon = when { paid -> R.drawable.detail_check; due -> R.drawable.detail_clock; else -> R.drawable.tab_plan },
                tint = tint,
                background = when { paid -> PaidBackground; due -> DueBackground; else -> Divider },
                status = row.status.replaceFirstChar { it.uppercase() },
            )
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    HorizontalDivider(color = Divider)
    Row(Modifier.fillMaxWidth().padding(vertical = QistSpacing.Content), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Small)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
