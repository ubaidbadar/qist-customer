package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.CustomerBackButton
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.RetailerContactActions
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun ProfileInformation(retailer: String, retailerPhone: String, privacy: Boolean, onBack: () -> Unit) {
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
        if (privacy) CustomerPrivacy() else HelpAndSupport(retailer, retailerPhone)
    }
}

@Composable
private fun HelpAndSupport(retailer: String, phone: String) {
    var expanded by rememberSaveable { mutableIntStateOf(0) }
    val questions = listOf(
        copy("When is my installment due?", "میری قسط کب واجب ہے؟") to copy("Open the Plan tab to see every due date and amount.", "ہر قسط کی تاریخ اور رقم منصوبے میں دیکھیں۔"),
        copy("How do I pay?", "میں ادائیگی کیسے کروں؟") to copy("Ask your retailer which payment methods they accept.", "ادائیگی کے طریقے اپنے دکاندار سے پوچھیں۔"),
        copy("Where can I see my receipts?", "رسیدیں کہاں دیکھوں؟") to copy("Open Payments and tap a transaction to see its details.", "ادائیگیوں میں کسی اندراج پر ٹیپ کر کے تفصیل دیکھیں۔"),
        copy("Why is my phone locked?", "میرا فون کیوں لاک ہے؟") to copy("Contact your retailer to check your plan and phone status.", "اپنے منصوبے اور فون کی حالت کے لیے دکاندار سے رابطہ کریں۔"),
        copy("How do I change my phone number?", "فون نمبر کیسے بدلوں؟") to copy("Ask your retailer to update the phone number on your plan.", "اپنے دکاندار سے منصوبے کا فون نمبر بدلنے کو کہیں۔"),
    )
    Panel {
        Caption(copy("Your retailer can help with payments", "ادائیگیوں میں آپ کا دکاندار مدد کر سکتا ہے"))
        Text(retailer, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        RetailerContactActions(phone)
    }
    Panel {
        Text(copy("Common questions", "عام سوالات"), style = MaterialTheme.typography.titleLarge)
        questions.forEachIndexed { index, (question, answer) ->
            if (index > 0) HorizontalDivider(color = Divider)
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = if (expanded == index) -1 else index }.heightIn(min = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(question, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(if (expanded == index) "⌃" else "⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded == index) Box(Modifier.padding(bottom = 14.dp)) { Caption(answer) }
        }
    }
    val context = LocalContext.current
    val version = remember(context) { appVersion(context.packageManager, context.packageName) }
    Text(
        copy("Qist customer app · Version $version", "قسط کسٹمر ایپ · ورژن $version"),
        Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Suppress("DEPRECATION")
private fun appVersion(manager: android.content.pm.PackageManager, packageName: String): String =
    manager.getPackageInfo(packageName, 0).versionName.orEmpty()
