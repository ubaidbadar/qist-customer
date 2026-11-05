package pk.qist.customer.ui.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.components.MutedCopy
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Canvas

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
