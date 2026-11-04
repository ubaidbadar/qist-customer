package pk.qist.device.customer

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun PrivacyConsent(onPolicy: () -> Unit, onAgree: () -> Unit, onDecline: () -> Unit) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenBadge(R.drawable.detail_shield, Color.White, Blue, 56)
            Text(copy("Your privacy matters", "آپ کی رازداری اہم ہے"), style = MaterialTheme.typography.headlineMedium)
            MutedCopy(copy("Here is what Qist uses and why.", "قسط کیا استعمال کرتا ہے اور کیوں۔"))
            ConsentItem(R.drawable.phone_outline, copy("Your phone number", "آپ کا فون نمبر"), copy("To sign you in with a code.", "کوڈ کے ذریعے سائن ان کے لیے۔"))
            ConsentItem(R.drawable.tab_plan, copy("Your plan and payments", "آپ کا منصوبہ اور ادائیگیاں"), copy("To show your due dates, amounts and receipts.", "تاریخیں، رقوم اور رسیدیں دکھانے کے لیے۔"))
            ConsentItem(R.drawable.detail_bell, copy("Notifications (optional)", "اطلاعات (اختیاری)"), copy("For reminders before an installment is due.", "قسط واجب ہونے سے پہلے یاد دہانی کے لیے۔"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val label = copy("I have read the privacy policy", "میں نے رازداری کی پالیسی پڑھ لی ہے")
            Checkbox(checked = accepted, onCheckedChange = { accepted = it }, modifier = Modifier.semantics { contentDescription = label })
            Text(copy("I have read the", "میں نے پڑھ لی ہے"))
            TextButton(onClick = onPolicy, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(copy("Privacy policy", "رازداری کی پالیسی"))
            }
        }
        Button(onClick = onAgree, enabled = accepted, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(copy("Agree and continue", "قبول کریں اور جاری رکھیں"))
        }
        TextButton(onClick = onDecline, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(copy("Decline", "انکار کریں"))
        }
    }
}

@Composable
private fun ConsentItem(icon: Int, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenBadge(icon, Blue, Color(0xFFEDF0FF), 44)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            MutedCopy(description)
        }
    }
}

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
                ScreenBadge(R.drawable.detail_check, Color(0xFF08674D), Color(0xFFD9F4EB), 64)
                MutedCopy(copy("Payment received", "ادائیگی موصول ہوئی"))
                Text(amount, style = MaterialTheme.typography.headlineLarge)
                MutedCopy(payment.date)
            }
        }
        Panel {
            facts.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = Color(0xFFE4E9F4))
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { MutedCopy(label) }
                    Text(value, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PaymentPage(title: String, onBack: () -> Unit, actions: @Composable ColumnScope.() -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledIconButton(onClick = onBack, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Navy)) {
                Icon(painterResource(R.drawable.notice_back), contentDescription = copy("Back", "واپس"))
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = body)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = actions)
    }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), border = BorderStroke(1.dp, Color(0xFFBFCBE4))) {
        Text(label, color = Navy)
    }
}

@Composable
private fun ScreenBadge(icon: Int, color: Color, background: Color, size: Int) {
    Box(Modifier.size(size.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = color)
    }
}

@Composable
private fun MutedCopy(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}
