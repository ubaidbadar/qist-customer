package pk.qist.customer.ui.payments

import android.content.Intent
import android.content.ActivityNotFoundException

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentSample
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.components.SecondaryAction
import pk.qist.customer.ui.components.CustomerSnackbarHost
import pk.qist.customer.ui.components.accountMessage
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.PaymentPage
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.localization.LocalUrdu
import pk.qist.customer.ui.localization.paymentLabel
import pk.qist.customer.ui.localization.paymentMethodLabel
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun CustomerReceipt(data: DashboardSample, payment: PaymentSample, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var saving by remember { mutableStateOf(false) }
    val id = payment.receiptNumber.toString()
    val amount = copy("Rs. ${payment.amount}", "روپے ${payment.amount}")
    val title = copy("Receipt", "رسید")
    val singleInstallment = payment.kind == "installment" && payment.allocations.size == 1
    val facts = listOf(
        (if (singleInstallment) copy("Installment", "قسط") else copy("Payment", "ادائیگی")) to if (singleInstallment)
            copy("${payment.allocations.single()} of ${data.total}", "${data.total} میں سے ${payment.allocations.single()}") else paymentLabel(payment, LocalUrdu.current),
        copy("Phone", "فون") to data.model,
        copy("Payment method", "ادائیگی کا طریقہ") to paymentMethodLabel(payment.method, LocalUrdu.current),
        copy("Received by", "وصول کنندہ") to payment.receivedBy.ifBlank { data.retailer },
        copy("Receipt no.", "رسید نمبر") to id,
    ) + if (payment.reference.isNotBlank()) listOf(copy("Reference", "حوالہ") to payment.reference) else emptyList()
    val receipt = buildString {
        appendLine("Qist · " + copy("Receipt", "رسید"))
        appendLine("$amount · ${payment.date}")
        facts.forEach { (label, value) -> appendLine("$label: $value") }
    }
    val error = copy("Could not save the receipt. Please try again.", "رسید محفوظ نہیں ہو سکی۔ دوبارہ کوشش کریں۔")
    val saved = copy("Receipt saved", "رسید محفوظ ہو گئی")
    val view = copy("View", "دیکھیں")
    val unavailable = copy("No app is available to open this receipt.", "اس رسید کو کھولنے کے لیے کوئی ایپ دستیاب نہیں۔")
    val shareUnavailable = copy("No app is available to share this receipt.", "اس رسید کو شیئر کرنے کے لیے کوئی ایپ دستیاب نہیں۔")
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) saving = false
        else scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(receipt) }
                }
                saving = false
                if (snackbar.accountMessage(saved, view) == SnackbarResult.ActionPerformed) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/plain")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    } catch (_: ActivityNotFoundException) { snackbar.accountMessage(unavailable, error = true) }
                    catch (_: SecurityException) { snackbar.accountMessage(unavailable, error = true) }
                }
            } catch (_: IOException) { snackbar.accountMessage(error, error = true) }
            catch (_: SecurityException) { snackbar.accountMessage(error, error = true) }
            finally { saving = false }
        }
    }
    PaymentPage(title, onBack, feedback = { CustomerSnackbarHost(snackbar) }, actions = {
        Pill(copy("Share receipt", "رسید شیئر کریں"), {
            val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, receipt)
            try { context.startActivity(Intent.createChooser(intent, title)) }
            catch (_: ActivityNotFoundException) { scope.launch { snackbar.accountMessage(shareUnavailable, error = true) } }
            catch (_: SecurityException) { scope.launch { snackbar.accountMessage(shareUnavailable, error = true) } }
        }, icon = R.drawable.detail_share)
        SecondaryAction(if (saving) copy("Saving…", "محفوظ ہو رہی ہے…") else copy("Download", "ڈاؤن لوڈ کریں"), {
            saving = true
            try { download.launch("qist-receipt-$id.txt") }
            catch (_: ActivityNotFoundException) { saving = false; scope.launch { snackbar.accountMessage(error, error = true) } }
        }, icon = R.drawable.state_download, busy = saving)
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
                    Text(value, Modifier.weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
