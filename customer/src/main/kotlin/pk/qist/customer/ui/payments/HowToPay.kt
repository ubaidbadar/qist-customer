package pk.qist.customer.ui.payments

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import pk.qist.customer.data.CustomerPaymentMethod
import pk.qist.customer.data.CustomerPlan
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun HowToPay(plan: CustomerPlan, onBack: () -> Unit) {
    PaymentPage(copy("How to pay", "ادائیگی کیسے کریں"), onBack, actions = {
        RetailerContactActions(plan.retailerPhone, paymentProof = true, retailer = plan.retailer)
    }) {
        Surface(color = Navy, contentColor = MaterialTheme.colorScheme.surface, shape = QistShapes.Card) {
            Column(Modifier.fillMaxWidth().padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                val due = plan.nextInstallment
                Text(copy("Amount due", "واجب الادا رقم"), style = MaterialTheme.typography.bodyMedium)
                Text(money(due?.remaining ?: 0), style = MaterialTheme.typography.headlineLarge)
                if (due != null) Text(copy("Due ${due.dueDate}", "${due.dueDate} تک واجب"))
            }
        }
        Text(copy("Pay ${plan.retailer} using any of these", "${plan.retailer} کو ان طریقوں سے ادائیگی کریں"), style = MaterialTheme.typography.titleMedium)
        if (plan.paymentMethods.isEmpty()) Panel {
            Caption(copy("Your retailer has not added payment instructions yet. Contact them before paying.", "دکاندار نے ابھی ادائیگی کی تفصیلات شامل نہیں کیں۔ ادائیگی سے پہلے رابطہ کریں۔"))
        }
        plan.paymentMethods.forEach { method -> PaymentMethodCard(method, plan.retailerAddress) }
        Panel {
            Caption(copy("Qist does not process payments. Pay your retailer directly and keep your receipt. Sending proof does not confirm payment; refresh after your retailer records it.", "قسط ادائیگی وصول نہیں کرتا۔ دکاندار کو براہ راست ادائیگی کریں اور رسید محفوظ رکھیں۔ ثبوت بھیجنا ادائیگی کی تصدیق نہیں ہے؛ دکاندار کے اندراج کے بعد تازہ کریں۔"))
        }
    }
}

@Composable
private fun PaymentMethodCard(method: CustomerPaymentMethod, address: String) {
    val context = LocalContext.current
    val copied = copy("Account number copied", "اکاؤنٹ نمبر کاپی ہو گیا")
    Panel {
        Text(paymentMethodLabel(method.kind), style = MaterialTheme.typography.titleLarge)
        if (method.kind == "cash") {
            Caption(address.ifBlank { copy("Contact your retailer for the shop address.", "دکان کے پتے کے لیے دکاندار سے رابطہ کریں۔") })
        } else {
            if (method.bankName.isNotBlank()) Caption(method.bankName)
            Caption(method.accountTitle)
            Text(method.accountNumber, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Account number", method.accountNumber))
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                }
            }) { Text(copy("Copy", "کاپی کریں")) }
        }
    }
}
