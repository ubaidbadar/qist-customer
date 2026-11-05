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
internal fun CustomerPrivacy() {
    val context = LocalContext.current
    val sections = listOf(
        "About this policy" to "This policy applies to Qist Customer (Android package pk.qist.customer), provided by Ubaid Badar. The connected app lets registered customers view their retailer's installment plans, recorded payments, receipts, and the phone's last acknowledged status.",
        "Sign-in and WhatsApp verification" to "The app sends your phone number and password over HTTPS to the Qist backend to authenticate you. Passwords are stored on the server as Argon2 hashes, not plaintext. If you request first-password setup or password recovery, Qist sends your phone number and a one-time verification code to Meta's WhatsApp Business Platform for delivery to you. Codes expire after five minutes and are stored as server-keyed hashes. Meta handles WhatsApp delivery under its own policies. We do not use WhatsApp to send unsolicited marketing through this flow.",
        "Customer and financial records" to "The backend matches your verified phone number to customer records registered by your retailers. The app retrieves your name, phone number, plan and installment amounts, due dates, paid amounts, late fees, payment receipts, retailer contact details, and the last device lock state recorded by the retailer's system. It does not display other customers' records, CNICs, guarantor information, retailer inventory costs, or device-enrollment credentials. Ask your retailer to correct inaccurate registration or payment records.",
        "Local storage and permissions" to "The app uses internet access to communicate with Qist and to open links you choose. It keeps an encrypted sign-in token protected by Android Keystore, privacy acceptance, an in-app reminder preference, and read-notice identifiers on your device. Passwords and verification codes are not persisted by the app, and financial records are not cached to disk by this implementation. Backup is disabled for this app. Android or apps you choose may have separate data-handling practices.",
        "Reminders and device status" to "In-app notices are derived from your retrieved due dates, recorded payments, and last acknowledged phone status. The reminder switch does not register you for remote push notifications. Qist Customer does not enroll, lock, unlock, monitor, or manage your device. The displayed phone state may change when the separate retailer/device system reports a new acknowledgment; it is not a real-time connectivity guarantee.",
        "Payments, contact actions, and receipts" to "Qist Customer does not transfer money, approve financing, or process payments. Payments are made directly to your retailer and recorded by them. Call opens your dialer; WhatsApp contact opens a retailer chat, and a call/message is made only if you choose to continue. Download uses Android's file picker to save a text receipt where you choose. Share opens Android's share sheet; the destination you select receives the receipt text under its own policies. Exported receipts remain until you delete them.",
        "Retention, sign-out, and deletion requests" to "Customer-login sessions expire after 30 days and may be revoked sooner. Signing out removes the sign-in token on this device and attempts to revoke that session on the server; if the device is offline, server expiry still applies. Resetting the password revokes all customer-login sessions. Preferences remain until you clear app storage or uninstall. For customer-login account deletion or other privacy requests, contact m.ubaidbadar@gmail.com so we can verify ownership and handle the request. Retailer transaction and contract records are separate business records; contact the relevant retailer about correction or deletion and any applicable retention requirements. No fixed retention period for those records is promised here.",
        "Support, analytics, and advertising" to "If you email us, we receive your email address and the information you choose to provide, and use it to respond. Avoid sending passwords, verification codes, identity documents, or sensitive payment information unless necessary. This implementation includes no advertising or analytics SDK, and does not sell customer data through an advertising feature.",
        "Changes to this policy" to "We will update this policy and applicable store disclosures when the app's data handling changes. Contact us with questions about which features are enabled in your installed version.",
        "Privacy contact" to "For privacy questions, customer-login account deletion, or other requests, email m.ubaidbadar@gmail.com. Developer: Ubaid Badar.",
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
