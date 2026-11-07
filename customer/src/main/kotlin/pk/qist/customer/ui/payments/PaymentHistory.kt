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
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun PaymentHistory(data: DashboardSample, busy: Boolean, onMore: () -> Unit, onReceipt: (PaymentSample) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        Panel(Modifier.weight(1f)) {
            Caption(copy("Total paid", "کل ادا شدہ"))
            Text(money(data.paidTotal), color = PaidGreen, style = MaterialTheme.typography.titleLarge)
        }
        Panel(Modifier.weight(1f)) {
            Caption(copy("Remaining", "باقی"))
            Text(money(data.remaining), style = MaterialTheme.typography.titleLarge)
        }
    }
    Panel {
        Text(copy("History", "ادائیگیوں کی تفصیل"), style = MaterialTheme.typography.titleLarge)
        if (data.payments.isEmpty()) Text(copy("No payments recorded yet.", "ابھی کوئی ادائیگی درج نہیں۔"))
        data.payments.forEachIndexed { index, payment ->
            if (index > 0) HorizontalDivider(color = Divider)
            DetailRow(
                title = copy(payment.label, payment.label),
                subtitle = "${payment.date} · ${payment.method}",
                value = copy("Rs. ${payment.amount}", "روپے ${payment.amount}"),
                icon = R.drawable.detail_check, tint = PaidGreen, background = PaidBackground,
                valueColor = PaidGreen, onClick = { onReceipt(payment) },
            )
        }
        if (data.nextCursor != null) TextButton(onClick = onMore, enabled = !busy) { Text(copy("Load more", "مزید دکھائیں")) }
    }
}
