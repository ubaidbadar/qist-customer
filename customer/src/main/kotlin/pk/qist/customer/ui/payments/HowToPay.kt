package pk.qist.customer.ui.payments

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun HowToPay(data: DashboardSample, onBack: () -> Unit) {
    var contact by rememberSaveable { mutableStateOf(false) }
    var whatsApp by remember { mutableStateOf(false) }
    var methods by rememberSaveable { mutableStateOf(false) }
    PaymentPage(copy("How to pay", "ادائیگی کیسے کریں"), onBack, actions = {
        Button(onClick = { whatsApp = false; contact = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = androidx.compose.foundation.shape.CircleShape) {
            Icon(painterResource(R.drawable.payment_call), null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(copy("Call ${data.retailer}", "${data.retailer} کو کال کریں"))
        }
        OutlinedButton(onClick = { whatsApp = true; contact = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = androidx.compose.foundation.shape.CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder)) {
            Icon(painterResource(R.drawable.payment_chat), null, Modifier.size(20.dp), tint = Navy)
            Spacer(Modifier.width(8.dp))
            Text(copy("Send payment proof on WhatsApp", "واٹس ایپ پر ادائیگی کا ثبوت بھیجیں"), color = Navy)
        }
    }) {
        Surface(color = Navy, contentColor = Color.White, shape = QistShapes.Card) {
            FlowRow(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(copy("Amount due", "واجب الادا رقم"), color = Muted)
                    Text(copy("Rs. ${data.amount}", "روپے ${data.amount}"), style = MaterialTheme.typography.headlineLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(copy("Due date", "آخری تاریخ"), color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Text(if (data.completed) "—" else data.dueDate, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(copy("Pay ${data.retailer} using any of these", "${data.retailer} کو ان طریقوں سے ادا کریں"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { methods = true }) {
                Icon(painterResource(R.drawable.tab_payments), copy("View payment methods", "ادائیگی کے طریقے دیکھیں"), tint = Blue)
            }
        }
        PaymentMethods(data)
        PaymentNotice()
    }
    if (methods) PaymentMethodsSheet(data) { methods = false }
    if (contact) RetailerContactDialog(data.retailerPhone, whatsApp) { contact = false }
}
