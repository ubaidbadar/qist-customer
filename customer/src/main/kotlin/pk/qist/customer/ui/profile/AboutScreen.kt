package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import pk.qist.customer.BuildConfig
import pk.qist.customer.R
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun AboutScreen(busy: Boolean, error: String?, onBack: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit, onDelete: () -> Unit) {
    val checkForUpdate = pk.qist.customer.platform.LocalCheckForUpdate.current
    var deleting by rememberSaveable { mutableStateOf(false) }
    PaymentPage(copy("About", "تعارف"), onBack, actions = {}) {
        Panel {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).background(Brush.linearGradient(listOf(BrandGradientStart, Blue)), CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(14.dp))
                Text("Qist", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Caption(copy("Version ${BuildConfig.VERSION_NAME}", "ورژن ${BuildConfig.VERSION_NAME}"))
            }
        }
        Panel {
            DetailRow(copy("Check for updates", "اپ ڈیٹ چیک کریں"), "", "", R.drawable.state_download, onClick = checkForUpdate)
            HorizontalDivider(color = Divider)
            DetailRow(copy("Terms of service", "استعمال کی شرائط"), "", "", R.drawable.detail_document, onClick = onTerms)
            HorizontalDivider(color = Divider)
            DetailRow(copy("Privacy policy", "رازداری کی پالیسی"), "", "", R.drawable.detail_document, onClick = onPrivacy)
        }
        Panel {
            DetailRow(copy("Delete my data", "میرا ڈیٹا حذف کریں"), "", "", R.drawable.detail_delete,
                titleColor = Destructive, tint = Destructive, background = DestructiveBackground, onClick = { deleting = true })
        }
    }
    if (deleting) Dialog(onDismissRequest = { if (!busy) deleting = false }) {
        Surface(shape = QistShapes.Dialog, color = Color.White) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ScreenBadge(R.drawable.detail_delete, Destructive, DestructiveBackground, 64)
                Text(copy("Delete your data?", "اپنا ڈیٹا حذف کریں؟"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(copy("This removes your Qist app sign-in account and saved account data on this device, and signs you out. Your retailer's purchase and payment records are not affected. This cannot be undone.",
                    "اس سے آپ کا قسط ایپ لاگ ان اکاؤنٹ اور اس فون پر محفوظ اکاؤنٹ ڈیٹا حذف ہو گا، اور آپ لاگ آؤٹ ہو جائیں گے۔ دکاندار کے خریداری اور ادائیگی کے ریکارڈ متاثر نہیں ہوں گے۔ اسے واپس نہیں کیا جا سکتا۔"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { deleting = false }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Text(copy("Cancel", "منسوخ"))
                    }
                    Button(onClick = onDelete, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Destructive)) {
                        if (busy) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }
                        Text(if (busy) copy("Deleting…", "حذف ہو رہا ہے…") else copy("Delete", "حذف کریں"))
                    }
                }
            }
        }
    }
}

@Composable
internal fun TermsOfService(onBack: () -> Unit) {
    PaymentPage(copy("Terms of service", "استعمال کی شرائط"), onBack, actions = {}) {
        Panel {
            Text(copy("Using Qist", "قسط کا استعمال"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Caption(copy("Qist shows the installment plans and payment records your retailer maintains. Use the phone number registered with your retailer and keep your password and verification codes private.",
                "قسط وہ اقساط اور ادائیگیاں دکھاتا ہے جو دکاندار درج کرتا ہے۔ دکاندار کے پاس رجسٹرڈ فون نمبر استعمال کریں اور اپنا پاس ورڈ اور تصدیقی کوڈ خفیہ رکھیں۔"))
        }
        Panel {
            Text(copy("Your retailer and payments", "آپ کا دکاندار اور ادائیگیاں"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Caption(copy("Your purchase and installment agreement is with your retailer. Pay the retailer directly using their provided details and keep your receipts. Qist does not process payments. Ask your retailer about due amounts, corrections or phone lock status.",
                "خریداری اور اقساط کا معاہدہ آپ کے دکاندار کے ساتھ ہے۔ ان کی دی گئی تفصیل پر براہ راست ادائیگی کریں اور رسیدیں محفوظ رکھیں۔ قسط ادائیگی وصول نہیں کرتا۔ واجب رقم، درستگی یا فون کی حالت کے لیے دکاندار سے رابطہ کریں۔"))
        }
        Panel {
            Text(copy("Saved data and your choices", "محفوظ ڈیٹا اور آپ کے اختیارات"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Caption(copy("Offline screens show the last saved information and may be out of date. Refresh when connected before relying on a balance or phone status. Delete my data removes your app account and saved copy on this device; it does not cancel a purchase or remove retailer records. For privacy questions, email m.ubaidbadar@gmail.com.",
                "آف لائن اسکرینیں آخری محفوظ معلومات دکھاتی ہیں جو پرانی ہو سکتی ہیں۔ بیلنس یا فون کی حالت پر انحصار کرنے سے پہلے انٹرنیٹ کے ساتھ تازہ کریں۔ میرا ڈیٹا حذف کریں سے ایپ اکاؤنٹ اور اس فون کی محفوظ نقل حذف ہوتی ہے؛ خریداری منسوخ یا دکاندار کا ریکارڈ حذف نہیں ہوتا۔ رازداری کے سوالات کے لیے m.ubaidbadar@gmail.com پر ای میل کریں۔"))
        }
    }
}
