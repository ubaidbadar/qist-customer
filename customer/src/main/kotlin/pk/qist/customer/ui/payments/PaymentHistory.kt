package pk.qist.customer.ui.payments

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.DetailRow
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.money
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen

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
