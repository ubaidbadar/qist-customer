package pk.qist.customer.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pk.qist.customer.R
import pk.qist.customer.data.CustomerPlan
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun PlanDetails(plan: CustomerPlan?) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        item("heading") { Text(copy("My plan", "میرا منصوبہ"), style = MaterialTheme.typography.headlineMedium) }
        if (plan == null) item("empty") { CustomerEmptyState(copy("No installment plan is available yet.", "ابھی قسطوں کا منصوبہ دستیاب نہیں۔")) }
        else {
            item("summary") { Panel {
                DetailRow(plan.deviceLabel, plan.retailer, "", R.drawable.phone_outline)
                Fact(copy("Status", "حالت"), plan.status)
                Fact(copy("Total price", "کل قیمت"), money(plan.downPayment + plan.totalPayable))
                Fact(copy("Down payment", "پیشگی رقم"), money(plan.downPayment))
                Fact(copy("Late fees", "تاخیر کی فیس"), money(plan.lateFees))
                Fact(copy("Remaining", "باقی"), money(plan.remaining))
                Fact(copy("Schedule", "شیڈول"), "${plan.count} · ${plan.frequency}")
            } }
            if (plan.status == "cancelled") item("cancelled") { Panel {
                Text(copy("Plan cancelled", "منصوبہ منسوخ"), style = MaterialTheme.typography.titleMedium)
                Caption(copy("No scheduled payment is due.", "کوئی طے شدہ ادائیگی واجب نہیں۔"))
            } }
            else items(plan.installments, key = { it.number }, contentType = { "installment" }) { installment -> Panel {
                val paid = installment.status == "paid"
                DetailRow(copy("Installment ${installment.number}", "قسط ${installment.number}"), installment.dueDate, money(installment.amount + installment.lateFee),
                    if (paid) R.drawable.detail_check else R.drawable.detail_clock, tint = if (paid) PaidGreen else DueAmber,
                    background = if (paid) PaidBackground else DueBackground, status = installment.status)
                Fact(copy("Paid", "ادا شدہ"), money(installment.paidAmount))
                Fact(copy("Remaining", "باقی"), money(installment.remaining))
            } }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    HorizontalDivider(color = Divider)
    Row(Modifier.fillMaxWidth().padding(vertical = QistSpacing.Item), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
