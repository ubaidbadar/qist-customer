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
import pk.qist.customer.ui.components.AccountEmptyState
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

@Composable
internal fun EmptyPaymentHistory(error: String? = null) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(QistSpacing.Screen)) {
            Text(copy("Payments", "ادائیگیاں"), style = MaterialTheme.typography.headlineMedium)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            AccountEmptyState(R.drawable.tab_payments, copy("No payments yet", "ابھی کوئی ادائیگی نہیں"),
                copy("Your payments will appear here once your retailer records them.", "دکاندار کے درج کرنے کے بعد آپ کی ادائیگیاں یہاں دکھائی دیں گی۔"),
                Modifier.fillMaxWidth().heightIn(min = (viewport - 100.dp).coerceAtLeast(300.dp)))
        }
    }
}
