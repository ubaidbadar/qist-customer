package pk.qist.customer.ui.payments

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import pk.qist.customer.data.CustomerPlan
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy

@Composable
internal fun HowToPay(plan: CustomerPlan, onBack: () -> Unit) {
    PaymentPage(copy("How to pay", "ادائیگی کیسے کریں"), onBack, actions = { RetailerContactActions(plan.retailerPhone) }) {
        Panel {
            val due = plan.nextInstallment
            Caption(copy("Amount due", "واجب الادا رقم"))
            Text(money(due?.remaining ?: 0), style = MaterialTheme.typography.headlineLarge)
            if (due != null) Caption(copy("Due ${due.dueDate}", "${due.dueDate} تک واجب"))
        }
        Panel {
            Text(plan.retailer, style = MaterialTheme.typography.titleLarge)
            if (plan.retailerAddress.isNotBlank()) Caption(plan.retailerAddress)
            Caption(copy("Contact your retailer to confirm the payment methods they accept. Pay your retailer directly. Once they record your payment, refresh Qist to see your receipt.", "ادائیگی کا طریقہ دکاندار سے معلوم کریں اور براہ راست اسے ادائیگی کریں۔ ادائیگی درج ہونے کے بعد رسید دیکھنے کے لیے قسط کو تازہ کریں۔"))
        }
    }
}
