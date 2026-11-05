package pk.qist.customer.ui.payments

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.MutedCopy
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.PaymentPage
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.components.SecondaryAction
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Muted
import pk.qist.customer.ui.theme.Navy

@Composable
internal fun HowToPay(data: DashboardSample, onBack: () -> Unit) {
    var contact by rememberSaveable { mutableStateOf(false) }
    PaymentPage(copy("How to pay", "ادائیگی کیسے کریں"), onBack, actions = {
        Pill(copy("Call ${data.retailer}", "${data.retailer} کو کال کریں"), { contact = true })
        SecondaryAction(copy("WhatsApp", "واٹس ایپ")) { contact = true }
    }) {
        Surface(color = Navy, contentColor = Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(copy("Amount due", "واجب الادا رقم"), color = Muted)
                Text(copy("Rs. ${data.amount}", "روپے ${data.amount}"), style = MaterialTheme.typography.headlineLarge)
                Text(copy("Due ${data.dueDate}", "${data.dueDate} تک واجب"), color = Muted)
            }
        }
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PaymentStep(1, copy("Contact your retailer", "دکاندار سے رابطہ کریں"), copy("Call or message ${data.retailer} to arrange your payment.", "ادائیگی کے لیے ${data.retailer} سے رابطہ کریں۔"))
                PaymentStep(2, copy("Pay your retailer", "دکاندار کو ادائیگی کریں"), copy("Pay the amount due in the way your retailer accepts.", "دکاندار کے قبول کردہ طریقے سے ادائیگی کریں۔"))
                PaymentStep(3, copy("Get your receipt", "اپنی رسید حاصل کریں"), copy("Once your retailer records payment, you can find the receipt in Payments.", "دکاندار ادائیگی درج کرے تو رسید ادائیگیوں میں دیکھیں۔"))
            }
        }
    }
    if (contact) {
        AlertDialog(
            onDismissRequest = { contact = false },
            title = { Text(copy("Contact retailer", "دکاندار سے رابطہ")) },
            text = { Text(copy("This demo has no retailer phone number.", "ڈیمو میں دکاندار کا نمبر موجود نہیں۔")) },
            confirmButton = { TextButton(onClick = { contact = false }) { Text(copy("Close", "بند کریں")) } },
        )
    }
}

@Composable
private fun PaymentStep(number: Int, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(32.dp).background(Color(0xFFEDF0FF), CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = Blue, style = MaterialTheme.typography.labelLarge)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            MutedCopy(description)
        }
    }
}
