package pk.qist.customer.ui.plan

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.DetailRow
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.money
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen

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
private fun Fact(label: String, value: String) {
    HorizontalDivider(color = Divider)
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
