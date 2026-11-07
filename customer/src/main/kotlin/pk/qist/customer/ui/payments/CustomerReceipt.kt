package pk.qist.customer.ui.payments

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.PaymentPage
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.components.SecondaryAction
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun CustomerReceipt(data: DashboardSample, payment: PaymentSample, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val id = "DEMO-${payment.number.toString().padStart(3, '0')}"
    val amount = copy("Rs. ${payment.amount}", "روپے ${payment.amount}")
    val title = copy("Receipt", "رسید")
    val facts = listOf(
        copy("Installment", "قسط") to copy("${payment.number} of ${data.total}", "${data.total} میں سے ${payment.number}"),
        copy("Phone", "فون") to data.model,
        copy("Payment method", "ادائیگی کا طریقہ") to copy(payment.method, "نقد"),
        copy("Received by", "وصول کنندہ") to data.retailer,
        copy("Receipt no.", "رسید نمبر") to id,
    )
    val receipt = buildString {
        appendLine("Qist · " + copy("Demo receipt", "نمونہ رسید"))
        appendLine("$amount · ${payment.date}")
        facts.forEach { (label, value) -> appendLine("$label: $value") }
    }
    val error = copy("Could not save the receipt.", "رسید محفوظ نہیں ہو سکی۔")
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(receipt) }
                }
            } catch (_: IOException) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }
    PaymentPage(title, onBack, actions = {
        Pill(copy("Share receipt", "رسید شیئر کریں"), {
            val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, receipt)
            context.startActivity(Intent.createChooser(intent, title))
        })
        SecondaryAction(copy("Download", "ڈاؤن لوڈ کریں")) { download.launch("qist-receipt-$id.txt") }
    }) {
        Panel {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScreenBadge(R.drawable.detail_check, PaidGreen, PaidBackground, 64)
                Caption(copy("Payment received", "ادائیگی موصول ہوئی"))
                Text(amount, style = MaterialTheme.typography.headlineLarge)
                Caption(payment.date)
            }
        }
        Panel {
            facts.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = Divider)
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                    Box(Modifier.weight(1f)) { Caption(label) }
                    Text(value, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
