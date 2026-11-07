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
        "About this policy" to "Qist Customer (pk.qist.customer), provided by Ubaid Badar, lets registered customers view their retailer installment plans and payments.",
        "Sign-in and recovery" to "Your phone number and password are sent over HTTPS to the Qist backend to sign in. Setting or resetting a password uses a WhatsApp code delivered through Meta. Passwords and codes are not saved by this app.",
        "Account information" to "The app retrieves your name, registered phone number, available plans, installment dates and amounts, payment receipts, retailer contact details and last acknowledged device lock state. The backend limits access to records matching your verified phone at active retailers. No advertising or analytics SDK is included.",
        "Local storage" to "A session token is encrypted using an Android Keystore key and kept in storage excluded from Android backup. Privacy acceptance and local preferences remain on this device. Remote push notifications are not available in this version.",
        "Receipts and contact actions" to "Saving a receipt uses Android's file picker. Sharing a receipt sends its text to the destination you select. Calling a retailer opens the dialer; WhatsApp opens the retailer's WhatsApp link. Those services follow their own privacy policies.",
        "Retention and choices" to "Logging out revokes the current server session and removes this device's session token. Exported receipts remain until you delete them. Retailers retain contract and payment records; contact your retailer or the privacy contact below for correction or deletion requests and information about applicable retention requirements.",
        "Phone status and payments" to "This customer app displays the last acknowledged phone lock state and retailer payment guidance. It does not manage the device or process payments. Payment records are entered by your retailer.",
        "Privacy contact" to "For privacy questions or requests, email m.ubaidbadar@gmail.com. Developer: Ubaid Badar.",
    )
    Panel {
        Text(copy("Privacy policy", "رازداری کی پالیسی"), style = MaterialTheme.typography.titleLarge)
        Caption(copy("Last updated 7 November 2026", "آخری تازہ کاری 7 نومبر 2026"))
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
