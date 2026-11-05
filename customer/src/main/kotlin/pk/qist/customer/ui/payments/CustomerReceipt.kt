package pk.qist.customer.ui.payments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pk.qist.customer.R
import pk.qist.customer.data.*
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun CustomerReceipt(plan: CustomerPlan, payment: CustomerPayment, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val amount = money(payment.amount)
    val title = copy("Receipt", "رسید")
    val facts = listOf(
        copy("Receipt no.", "رسید نمبر") to "${payment.receiptNumber}",
        copy("Phone", "فون") to plan.deviceLabel,
        copy("Retailer", "دکاندار") to plan.retailer,
        copy("Received by", "وصول کنندہ") to payment.receivedByName,
        copy("Payment method", "ادائیگی کا طریقہ") to payment.method,
        copy("Payment type", "ادائیگی کی قسم") to payment.kind,
        copy("Installments", "اقساط") to payment.installmentNumbers.joinToString(", ").ifEmpty { "—" },
        copy("Reference", "حوالہ") to payment.reference.ifBlank { "—" },
    )
    val receipt = buildString {
        appendLine("Qist · $title"); appendLine("$amount · ${payment.receivedOn}")
        facts.forEach { (label, value) -> appendLine("$label: $value") }
        appendLine("Payment ID: ${payment.id}")
    }
    val error = copy("Could not save or share the receipt.", "رسید محفوظ یا شیئر نہیں ہو سکی۔")
    var saving by remember { mutableStateOf(false) }
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && !saving) scope.launch {
            saving = true
            try { withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(receipt) }
            } } catch (_: IOException) { Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
            catch (_: SecurityException) { Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
            finally { saving = false }
        }
    }
    PaymentPage(title, onBack, actions = {
        Pill(copy("Share receipt", "رسید شیئر کریں"), {
            try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, receipt), title)) }
            catch (_: ActivityNotFoundException) { Toast.makeText(context, error, Toast.LENGTH_SHORT).show() }
        })
        OutlinedButton(onClick = { download.launch("qist-receipt-${payment.receiptNumber}.txt") }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(copy("Download", "ڈاؤن لوڈ کریں")) }
    }) {
        Panel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                ScreenBadge(R.drawable.detail_check, PaidGreen, PaidBackground, 64)
                Caption(copy("Payment received", "ادائیگی موصول ہوئی"))
                Text(amount, style = MaterialTheme.typography.headlineLarge); Caption(payment.receivedOn)
            }
        }
        Panel { facts.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = QistSpacing.Item), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                Box(Modifier.weight(1f)) { Caption(label) }; Text(value, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(color = Divider)
        } }
    }
}
