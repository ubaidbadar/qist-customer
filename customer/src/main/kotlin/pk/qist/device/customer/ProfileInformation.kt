package pk.qist.device.customer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePicker(selectedUrdu: Boolean, onDismiss: () -> Unit, onApply: (Boolean) -> Unit) {
    var urdu by rememberSaveable { mutableStateOf(selectedUrdu) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = Navy.copy(alpha = 0.60f),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(copy("Choose language", "زبان منتخب کریں"), style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LanguageOption("English", "English", !urdu) { urdu = false }
                LanguageOption("اردو", "Urdu", urdu) { urdu = true }
            }
            Pill(copy("Apply", "لاگو کریں"), { onApply(urdu) })
        }
    }
}

@Composable
private fun LanguageOption(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFEDF0FF) else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Blue else Color(0xFFBFCBE4)),
    ) {
        Row(
            Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                ProfileCaption(subtitle)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
internal fun LogoutConfirmation(onDismiss: () -> Unit, onLogout: () -> Unit) {
    val destructive = Color(0xFFAA381D)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(64.dp).background(Color(0xFFFFE8E0), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.detail_logout), contentDescription = null, tint = destructive, modifier = Modifier.size(30.dp))
                }
                Text(copy("Log out?", "لاگ آؤٹ کریں؟"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    copy("You will need your phone number and a code to sign in again.", "دوبارہ سائن ان کرنے کے لیے فون نمبر اور کوڈ درکار ہوگا۔"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(copy("Cancel", "منسوخ"))
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = destructive, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text(copy("Log out", "لاگ آؤٹ")) }
                }
            }
        }
    }
}

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

@Composable
private fun DemoPrivacy() {
    val sections = listOf(
        copy("Information we collect", "جمع کی جانے والی معلومات") to copy("You enter a phone number to try demo sign-in. The demo uses sample payment and plan records.", "ڈیمو سائن ان کے لیے فون نمبر درج کیا جاتا ہے۔ ادائیگیوں اور منصوبے کا ڈیٹا نمونہ ہے۔"),
        copy("How we use your information", "معلومات کا استعمال") to copy("The demo displays your entered number in the app and saves your reminder preference on this device.", "ڈیمو درج کردہ نمبر ایپ میں دکھاتا ہے اور یاد دہانی کی ترجیح اسی فون پر محفوظ کرتا ہے۔"),
        copy("Phone management by your retailer", "دکاندار کی جانب سے فون کا انتظام") to copy("This customer demo displays sample phone status. It has no device management controls.", "کسٹمر ڈیمو فون کی نمونہ حالت دکھاتا ہے۔ اس میں فون کے انتظام کے اختیارات نہیں ہیں۔"),
        copy("Your choices", "آپ کے اختیارات") to copy("Change the demo reminder preference in Profile or log out to clear the number from the current session.", "پروفائل میں یاد دہانی کی ترجیح بدلیں یا موجودہ سیشن سے نمبر ہٹانے کے لیے لاگ آؤٹ کریں۔"),
        copy("Contact us", "ہم سے رابطہ") to copy("For plan or payment questions, contact your retailer.", "منصوبے یا ادائیگی سے متعلق سوالات کے لیے اپنے دکاندار سے رابطہ کریں۔"),
    )
    Panel {
        ProfileCaption(copy("Demo privacy notice", "ڈیمو رازداری کا نوٹس"))
        sections.forEach { (title, body) ->
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ProfileCaption(body)
        }
    }
    OutlinedButton(
        onClick = {},
        enabled = false,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) { Text(copy("Open full policy in browser", "مکمل پالیسی براؤزر میں کھولیں")) }
    ProfileCaption(copy("A published policy link is not configured for this demo.", "اس ڈیمو میں شائع شدہ پالیسی کا لنک موجود نہیں۔"))
}

@Composable
private fun ProfileCaption(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}
