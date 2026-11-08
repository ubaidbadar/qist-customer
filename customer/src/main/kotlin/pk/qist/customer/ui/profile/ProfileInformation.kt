package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.ExpansionChevron
import pk.qist.customer.ui.components.CustomerBackButton
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.RetailerContactDialog
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun ProfileInformation(data: DashboardSample, privacy: Boolean, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Canvas).systemBarsPadding().verticalScroll(rememberScrollState()).padding(QistSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(QistSpacing.Section),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
            CustomerBackButton(onBack)
            Text(
                if (privacy) copy("Privacy policy", "رازداری کی پالیسی") else copy("Help and support", "مدد اور معاونت"),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        if (privacy) PrivacyPolicy() else HelpAndSupport(data.retailer, data.retailerPhone, data.retailerAddress)
    }
}

@Composable
private fun HelpAndSupport(retailer: String, phone: String, address: String) {
    RetailerSupportCard(retailer, phone, address)
    CommonQuestions()
    val version = pk.qist.customer.BuildConfig.VERSION_NAME
    Text(
        copy("Qist customer app · Version $version", "قسط کسٹمر ایپ · ورژن $version"),
        Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun RetailerSupportCard(retailer: String, phone: String, address: String) {
    var contact by rememberSaveable { mutableStateOf("") }
    Panel {
        Caption(copy("Your retailer", "آپ کا دکاندار"))
        Text(retailer.ifBlank { copy("Retailer details unavailable", "دکاندار کی تفصیل دستیاب نہیں") }, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(copy("Contact your retailer for payments, receipts, plan changes and phone lock issues.", "ادائیگیوں، رسیدوں، منصوبے میں تبدیلی اور فون لاک کے مسائل کے لیے دکاندار سے رابطہ کریں۔"),
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Divider)
        Spacer(Modifier.height(16.dp))
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SupportDetail(copy("Shop address", "دکان کا پتہ"), address.ifBlank {
                    copy("Address not provided. Ask your retailer for the shop location.", "پتہ فراہم نہیں کیا گیا۔ دکان کا مقام اپنے دکاندار سے پوچھیں۔")
                })
                SupportDetail(copy("Phone number", "فون نمبر"), phone.ifBlank {
                    copy("Phone number not provided.", "فون نمبر فراہم نہیں کیا گیا۔")
                })
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
            Button(onClick = { contact = "call" }, enabled = phone.isNotBlank(), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(copy("Call", "کال کریں"))
            }
            OutlinedButton(onClick = { contact = "whatsapp" }, enabled = phone.isNotBlank(), modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(copy("WhatsApp", "واٹس ایپ"))
            }
        }
    }
    if (contact.isNotEmpty()) RetailerContactDialog(phone, whatsApp = contact == "whatsapp") { contact = "" }
}

@Composable
private fun SupportDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CommonQuestions() {
    var expanded by rememberSaveable { mutableIntStateOf(-1) }
    val questions = listOf(
        copy("When is my installment due?", "میری قسط کب واجب ہے؟") to copy("Open the Plan tab to see every due date, installment amount and payment status. Home also shows your next unpaid installment.", "ہر قسط کی تاریخ، رقم اور ادائیگی کی حالت منصوبے میں دیکھیں۔ ہوم پر اگلی غیر ادا شدہ قسط بھی دکھائی جاتی ہے۔"),
        copy("How do I pay?", "میں ادائیگی کیسے کروں؟") to copy("Tap How to pay on Home to see your retailer's payment methods. Pay your retailer directly and keep your receipt. Qist does not collect payments.", "ہوم پر ادائیگی کا طریقہ کھول کر دکاندار کے طریقے دیکھیں۔ دکاندار کو براہ راست ادائیگی کریں اور رسید محفوظ رکھیں۔ قسط ادائیگی وصول نہیں کرتا۔"),
        copy("Where can I see my receipts?", "رسیدیں کہاں دیکھوں؟") to copy("Open Payments and tap a recorded payment to view its receipt. You can share or save the receipt from that screen.", "ادائیگیوں میں کسی درج شدہ ادائیگی پر ٹیپ کر کے رسید دیکھیں۔ اسی اسکرین سے رسید شیئر یا محفوظ کر سکتے ہیں۔"),
        copy("I paid, but my payment is missing", "ادائیگی کی ہے مگر دکھائی نہیں دے رہی") to copy("Your retailer must record the payment before it appears in Qist. Send them your payment proof, then pull down to refresh after they confirm it.", "قسط میں دکھانے کے لیے دکاندار کو ادائیگی درج کرنا ہوتی ہے۔ ادائیگی کا ثبوت بھیجیں اور ان کی تصدیق کے بعد نیچے کھینچ کر تازہ کریں۔"),
        copy("Why is my phone locked?", "میرا فون کیوں لاک ہے؟") to copy("Contact your retailer to check any overdue installment and the phone's lock status. If you have already paid, share your receipt so they can verify the payment.", "تاخیر شدہ قسط اور فون لاک کی حالت جاننے کے لیے دکاندار سے رابطہ کریں۔ ادائیگی ہو چکی ہے تو تصدیق کے لیے رسید شیئر کریں۔"),
        copy("How do I change my phone number?", "فون نمبر کیسے بدلوں؟") to copy("Ask your retailer to update the phone number on your plan. You cannot edit the registered number in this app.", "اپنے دکاندار سے منصوبے کا فون نمبر بدلنے کو کہیں۔ رجسٹرڈ نمبر اس ایپ سے تبدیل نہیں کیا جا سکتا۔"),
        copy("Can I use Qist offline?", "کیا قسط انٹرنیٹ کے بغیر استعمال کر سکتا ہوں؟") to copy("When saved data is available, Qist shows it with an offline notice. Reconnect and pull down to refresh for the latest payments and phone status.", "محفوظ معلومات دستیاب ہوں تو قسط انہیں آف لائن اطلاع کے ساتھ دکھاتا ہے۔ تازہ ادائیگیوں اور فون کی حالت کے لیے انٹرنیٹ سے جڑ کر نیچے کھینچیں۔"),
    )
    val expandedLabel = copy("Expanded", "کھلا ہوا")
    val collapsedLabel = copy("Collapsed", "بند")
    Panel {
        Text(copy("Common questions", "عام سوالات"), style = MaterialTheme.typography.titleLarge)
        questions.forEachIndexed { index, (question, answer) ->
            if (index > 0) HorizontalDivider(color = Divider)
            Row(
                Modifier.fillMaxWidth().semantics { stateDescription = if (expanded == index) expandedLabel else collapsedLabel }
                    .clickable(role = Role.Button) { expanded = if (expanded == index) -1 else index }
                    .heightIn(min = 54.dp).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item),
            ) {
                Text(question, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                ExpansionChevron(expanded == index)
            }
            if (expanded == index) Text(answer, Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
