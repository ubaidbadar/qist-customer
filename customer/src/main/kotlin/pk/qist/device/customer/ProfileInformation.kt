package pk.qist.customer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
    val context = LocalContext.current
    val sections = listOf(
        "About this policy" to "This policy applies to Qist Customer (Android package pk.qist.customer), provided by Ubaid Badar. It describes the current demonstration version, which uses sample installment plans, payments, phone status and receipts.",
        "Information used by the app" to "You can enter a phone number and a demo verification code. They are used locally to demonstrate sign-in and display your number in the app. The app does not send SMS messages or create a real customer account. Phone numbers and codes may be included in Android's temporary saved screen state so the screen can be restored.",
        "Collection and sharing" to "The current app does not send your phone number, installment information or payment information to the developer or a backend server. It includes no advertising or analytics SDK. The plan, payment, retailer, phone-status and receipt records shown in this version are sample records.",
        "Local preferences and Android services" to "The app stores your privacy acceptance and notification preference locally. The notification preference is a demo setting; this version does not register for remote push messages. Android's backup and restore services may back up app preferences according to your device settings. Those services are governed by the platform provider's terms and privacy policies.",
        "Receipts you save or share" to "Downloading a receipt uses Android's file picker to save a text file to a location you choose. Sharing a receipt opens Android's share sheet. Only if you choose a destination will the selected app or service receive the receipt text. The selected provider handles that information under its own policies. Demo receipts are marked as demo data.",
        "Retention and your choices" to "Logging out clears the entered phone number and code from the current app session. Preferences remain until you clear the app's storage or uninstall it, subject to Android backup and restore settings. You can change the notification preference in Profile. Exported receipts remain in your selected storage location until you delete them there. Because this demo creates no server account, there is no server account record to delete.",
        "Support correspondence" to "If you email us, we receive your email address and the information you choose to send. We use that correspondence to respond to your request and address support or privacy concerns. Keep sensitive account, payment and identity information out of support messages unless necessary. You may request deletion of support correspondence, subject to applicable retention requirements.",
        "Phone status and retailer payments" to "The customer app displays sample phone status and payment guidance. It does not manage or lock your device, approve financing, transfer money or process payments. Retailer contact controls in this demo do not contact a real retailer.",
        "Changes to this policy" to "We will update this policy and the applicable Google Play disclosures before releasing features that change how user data is handled, such as real sign-in, backend synchronization, analytics or push notifications.",
        "Privacy contact" to "For privacy questions or requests, email m.ubaidbadar@gmail.com. Developer: Ubaid Badar.",
    )
    Panel {
        Text(copy("Privacy policy", "رازداری کی پالیسی"), style = MaterialTheme.typography.titleLarge)
        ProfileCaption(copy("Last updated 5 November 2026", "آخری تازہ کاری 5 نومبر 2026"))
        sections.forEach { (title, body) ->
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ProfileCaption(body)
        }
    }
    val browserError = copy("No browser is available to open the privacy policy.", "رازداری کی پالیسی کھولنے کے لیے براؤزر دستیاب نہیں۔")
    OutlinedButton(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qist-customer-privacy.tallpage4.chatgpt.site")))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, browserError, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) { Text(copy("Open full policy in browser", "مکمل پالیسی براؤزر میں کھولیں")) }
}

@Composable
private fun ProfileCaption(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}
