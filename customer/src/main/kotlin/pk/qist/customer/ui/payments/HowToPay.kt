package pk.qist.customer.ui.payments

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.PaymentPage
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.components.RetailerContactDialog
import pk.qist.customer.ui.components.SecondaryAction
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.BlueBackground
import pk.qist.customer.ui.theme.Muted
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun HowToPay(data: DashboardSample, onBack: () -> Unit) {
    var contact by rememberSaveable { mutableStateOf(false) }
    var whatsApp by remember { mutableStateOf(false) }
    PaymentPage(copy("How to pay", "ادائیگی کیسے کریں"), onBack, actions = {
        Pill(copy("Call ${data.retailer}", "${data.retailer} کو کال کریں"), { whatsApp = false; contact = true })
        SecondaryAction(copy("WhatsApp", "واٹس ایپ")) { whatsApp = true; contact = true }
    }) {
        Surface(color = Navy, contentColor = Color.White, shape = QistShapes.Card) {
            Column(Modifier.fillMaxWidth().padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(copy("Amount due", "واجب الادا رقم"), color = Muted)
                Text(copy("Rs. ${data.amount}", "روپے ${data.amount}"), style = MaterialTheme.typography.headlineLarge)
                Text(copy("Due ${data.dueDate}", "${data.dueDate} تک واجب"), color = Muted)
            }
        }
        if (data.paymentMethods.isNotEmpty()) Panel {
            Text(copy("Payment methods", "ادائیگی کے طریقے"), style = MaterialTheme.typography.titleLarge)
            data.paymentMethods.forEach { method ->
                Text(method.bank.ifBlank { method.kind.replaceFirstChar { it.uppercase() } }, style = MaterialTheme.typography.titleMedium)
                if (method.title.isNotEmpty()) Caption(method.title)
                if (method.number.isNotEmpty()) androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr,
                ) { androidx.compose.foundation.text.selection.SelectionContainer { Text(method.number) } }
            }
        }
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                PaymentStep(1, copy("Contact your retailer", "دکاندار سے رابطہ کریں"), copy("Call or message ${data.retailer} to arrange your payment.", "ادائیگی کے لیے ${data.retailer} سے رابطہ کریں۔"))
                PaymentStep(2, copy("Pay your retailer", "دکاندار کو ادائیگی کریں"), copy("Pay the amount due in the way your retailer accepts.", "دکاندار کے قبول کردہ طریقے سے ادائیگی کریں۔"))
                PaymentStep(3, copy("Get your receipt", "اپنی رسید حاصل کریں"), copy("Once your retailer records payment, you can find the receipt in Payments.", "دکاندار ادائیگی درج کرے تو رسید ادائیگیوں میں دیکھیں۔"))
            }
        }
    }
    if (contact) {
        RetailerContactDialog(data.retailerPhone, whatsApp) { contact = false }
    }
}

@Composable
private fun PaymentStep(number: Int, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(32.dp).background(BlueBackground, CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = Blue, style = MaterialTheme.typography.labelLarge)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Caption(description)
        }
    }
}
