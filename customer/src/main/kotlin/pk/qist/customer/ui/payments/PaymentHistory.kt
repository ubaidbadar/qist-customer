package pk.qist.customer.ui.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pk.qist.customer.R
import pk.qist.customer.data.*
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun PaymentHistory(data: CustomerOverview, loadingMore: Boolean, onLoadMore: () -> Unit, onReceipt: (CustomerPayment) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        item("heading") { Text(copy("Payments", "ادائیگیاں"), style = MaterialTheme.typography.headlineMedium) }
        item("totals") {
            Row(horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                Panel(Modifier.weight(1f)) { Caption(copy("Total paid", "کل ادا شدہ")); Text(money(data.plan?.totalPaid ?: 0), color = PaidGreen, style = MaterialTheme.typography.titleLarge) }
                Panel(Modifier.weight(1f)) { Caption(copy("Remaining", "باقی")); Text(money(data.plan?.remaining ?: 0), style = MaterialTheme.typography.titleLarge) }
            }
        }
        if (data.payments.items.isEmpty()) item("empty") { CustomerEmptyState(copy("Your retailer has not recorded a payment yet.", "آپ کے دکاندار نے ابھی ادائیگی درج نہیں کی۔")) }
        items(data.payments.items, key = { it.id }, contentType = { "payment" }) { payment -> Panel { PaymentRow(payment) { onReceipt(payment) } } }
        if (data.payments.nextCursor != null) item("more") { Pill(copy("Load more", "مزید دکھائیں"), onLoadMore, loading = loadingMore) }
    }
}

@Composable
internal fun PaymentRow(payment: CustomerPayment, onClick: () -> Unit) {
    DetailRow(if (payment.kind == "down_payment") copy("Advance payment", "پیشگی ادائیگی") else copy("Payment received", "ادائیگی موصول ہوئی"),
        "${payment.receivedOn} · #${payment.receiptNumber}", money(payment.amount), R.drawable.detail_check,
        tint = PaidGreen, background = PaidBackground, valueColor = PaidGreen, onClick = onClick)
}
