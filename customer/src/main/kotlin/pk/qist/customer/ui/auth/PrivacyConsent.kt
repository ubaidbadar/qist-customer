package pk.qist.customer.ui.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun PrivacyConsent(onPolicy: () -> Unit, onAgree: () -> Unit, onDecline: () -> Unit) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(QistSpacing.AuthScreen)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
            ScreenBadge(R.drawable.detail_shield, CardSurface, Blue, QistSize.ConsentBadge)
            Text(copy("Your privacy matters", "آپ کی رازداری اہم ہے"), style = MaterialTheme.typography.headlineSmall)
            Caption(copy("Here is what Qist uses and why.", "قسط کیا استعمال کرتا ہے اور کیوں۔"))
            ConsentItem(R.drawable.phone_outline, copy("Your phone number", "آپ کا فون نمبر"), copy("To sign in and recover your password via WhatsApp.", "لاگ ان اور واٹس ایپ سے پاس ورڈ بحال کرنے کے لیے۔"))
            ConsentItem(R.drawable.tab_plan, copy("Your plan and payments", "آپ کا منصوبہ اور ادائیگیاں"), copy("To show your due dates, amounts and receipts.", "تاریخیں، رقوم اور رسیدیں دکھانے کے لیے۔"))
            ConsentItem(R.drawable.detail_bell, copy("Notifications (optional)", "اطلاعات (اختیاری)"), copy("Remote notifications are not available in this version.", "اس ورژن میں ریموٹ اطلاعات دستیاب نہیں۔"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val label = copy("I have read the privacy policy", "میں نے رازداری کی پالیسی پڑھ لی ہے")
            Checkbox(checked = accepted, onCheckedChange = { accepted = it }, modifier = Modifier.semantics { contentDescription = label })
            Text(copy("I have read the", "میں نے پڑھ لی ہے"))
            TextButton(onClick = onPolicy, contentPadding = PaddingValues(horizontal = QistSpacing.Tiny)) {
                Text(copy("Privacy policy", "رازداری کی پالیسی"))
            }
        }
        Button(onClick = onAgree, enabled = accepted, modifier = Modifier.fillMaxWidth().heightIn(min = QistSize.AuthButton),
            colors = ButtonDefaults.buttonColors(disabledContainerColor = DisabledBackground, disabledContentColor = DisabledText),
        ) {
            Text(copy("Agree and continue", "قبول کریں اور جاری رکھیں"))
        }
        TextButton(onClick = onDecline, modifier = Modifier.fillMaxWidth().heightIn(min = QistSize.TouchTarget)) {
            Text(copy("Decline", "انکار کریں"))
        }
    }
}

@Composable
private fun ConsentItem(icon: Int, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(QistSpacing.Content)) {
        ScreenBadge(icon, Blue, BlueBackground, QistSize.CircleLarge)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Caption(description)
        }
    }
}
