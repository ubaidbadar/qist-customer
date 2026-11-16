package pk.qist.customer.ui.payments

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
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.ui.components.AccountMessage
import pk.qist.customer.ui.components.panelSection
import pk.qist.customer.ui.components.AccountEmptyState
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.DetailRow
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.money
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.paymentLabel
import pk.qist.customer.ui.localization.paymentMethodLabel
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun PaymentHistory(data: DashboardSample, busy: Boolean, message: String?, onMore: () -> Unit, onReceipt: (PaymentSample) -> Unit) {
    if (data.payments.isEmpty()) { EmptyPaymentHistory(message); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(QistSpacing.Screen)) {
        item(key = "title", contentType = "heading") {
            Text(copy("Payments", "ادائیگیاں"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(QistSpacing.Section))
            if (message != null) { AccountMessage(message); Spacer(Modifier.height(QistSpacing.Section)) }
        }
        item(key = "summary", contentType = "summary") {
            PaymentSummaryCards(
                paid = {
                    Caption(copy("Total paid", "کل ادا شدہ"))
                    Text(money(data.paidTotal), color = PaidGreen, style = MaterialTheme.typography.titleLarge)
                },
                remaining = {
                    Caption(copy("Remaining", "باقی"))
                    Text(money(data.remaining), style = MaterialTheme.typography.titleLarge)
                },
            )
            Spacer(Modifier.height(QistSpacing.Section))
        }
        panelSection("payments", data.payments, { it.id }, title = {
            Text(copy("History", "ادائیگیوں کی تفصیل"), style = MaterialTheme.typography.titleLarge)
        }, footer = if (data.nextCursor == null) null else ({
            TextButton(onClick = onMore, enabled = !busy) { Text(copy("Load more", "مزید دکھائیں")) }
        })) { payment ->
            Box(Modifier.testTag("payments-history-row")) {
                DetailRow(title = paymentLabel(payment, LocalUrdu.current), subtitle = "${payment.date} · ${paymentMethodLabel(payment.method, LocalUrdu.current)}",
                    value = copy("Rs. ${payment.amount}", "روپے ${payment.amount}"), icon = R.drawable.detail_check,
                    tint = PaidGreen, background = PaidBackground, valueColor = PaidGreen, onClick = { onReceipt(payment) })
            }
        }
    }
}


/** Keeps the paid/remaining cards identical in loading and loaded states. */
@Composable
internal fun PaymentSummaryCards(
    paid: @Composable ColumnScope.() -> Unit,
    remaining: @Composable ColumnScope.() -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        Panel(Modifier.weight(1f).testTag("payments-paid-card"), content = paid)
        Panel(Modifier.weight(1f).testTag("payments-remaining-card"), content = remaining)
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
