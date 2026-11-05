package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.ProfileCaption
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.Navy

@Composable
internal fun ProfileInformation(data: DashboardSample, privacy: Boolean, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Canvas).systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledIconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Navy),
            ) { Icon(painterResource(R.drawable.notice_back), contentDescription = copy("Back", "واپس")) }
            Text(
                if (privacy) copy("Privacy policy", "رازداری کی پالیسی") else copy("Help and support", "مدد اور معاونت"),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        if (privacy) DemoPrivacy() else HelpAndSupport(data.retailer)
    }
}

@Composable
private fun HelpAndSupport(retailer: String) {
    var expanded by rememberSaveable { mutableIntStateOf(0) }
    var contact by rememberSaveable { mutableStateOf(false) }
    val questions = listOf(
        copy("When is my installment due?", "میری قسط کب واجب ہے؟") to copy("Open the Plan tab to see every due date and amount.", "ہر قسط کی تاریخ اور رقم منصوبے میں دیکھیں۔"),
        copy("How do I pay?", "میں ادائیگی کیسے کروں؟") to copy("Ask your retailer which payment methods they accept.", "ادائیگی کے طریقے اپنے دکاندار سے پوچھیں۔"),
        copy("Where can I see my receipts?", "رسیدیں کہاں دیکھوں؟") to copy("Open Payments and tap a transaction to see its details.", "ادائیگیوں میں کسی اندراج پر ٹیپ کر کے تفصیل دیکھیں۔"),
        copy("Why is my phone locked?", "میرا فون کیوں لاک ہے؟") to copy("Contact your retailer to check your plan and phone status.", "اپنے منصوبے اور فون کی حالت کے لیے دکاندار سے رابطہ کریں۔"),
        copy("How do I change my phone number?", "فون نمبر کیسے بدلوں؟") to copy("Ask your retailer to update the phone number on your plan.", "اپنے دکاندار سے منصوبے کا فون نمبر بدلنے کو کہیں۔"),
    )
    Panel {
        ProfileCaption(copy("Your retailer can help with payments", "ادائیگیوں میں آپ کا دکاندار مدد کر سکتا ہے"))
        Text(retailer, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { contact = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(copy("Call", "کال کریں")) }
            OutlinedButton(onClick = { contact = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(copy("WhatsApp", "واٹس ایپ")) }
        }
    }
    Panel {
        Text(copy("Common questions", "عام سوالات"), style = MaterialTheme.typography.titleLarge)
        questions.forEachIndexed { index, (question, answer) ->
            if (index > 0) HorizontalDivider(color = Color(0xFFE4E9F4))
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = if (expanded == index) -1 else index }.heightIn(min = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(question, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(if (expanded == index) "⌃" else "⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded == index) Box(Modifier.padding(bottom = 14.dp)) { ProfileCaption(answer) }
        }
    }
    val context = LocalContext.current
    val version = remember(context) { appVersion(context.packageManager, context.packageName) }
    Text(
        copy("Qist customer app · Version $version", "قسط کسٹمر ایپ · ورژن $version"),
        Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (contact) {
        AlertDialog(
            onDismissRequest = { contact = false },
            title = { Text(copy("Contact retailer", "دکاندار سے رابطہ")) },
            text = { Text(copy("This demo has no retailer phone number.", "ڈیمو میں دکاندار کا نمبر موجود نہیں۔")) },
            confirmButton = { TextButton(onClick = { contact = false }) { Text(copy("Close", "بند کریں")) } },
        )
    }
}

@Suppress("DEPRECATION")
private fun appVersion(manager: android.content.pm.PackageManager, packageName: String): String =
    manager.getPackageInfo(packageName, 0).versionName.orEmpty()
