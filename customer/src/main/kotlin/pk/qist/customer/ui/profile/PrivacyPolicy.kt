package pk.qist.customer.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.components.money
import pk.qist.customer.ui.localization.copy

@Composable
internal fun DemoPrivacy() {
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
        Caption(copy("Last updated 5 November 2026", "آخری تازہ کاری 5 نومبر 2026"))
        sections.forEach { (title, body) ->
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Caption(body)
        }
    }
    val browserError = copy("No browser is available to open the privacy policy.", "رازداری کی پالیسی کھولنے کے لیے براؤزر دستیاب نہیں۔")
    OutlinedButton(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qist-a.web.app/privacy-policy")))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, browserError, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) { Text(copy("Open full policy in browser", "مکمل پالیسی براؤزر میں کھولیں")) }
}
