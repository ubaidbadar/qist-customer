package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.components.Badge
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Chevron
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Destructive
import pk.qist.customer.ui.theme.DestructiveBackground
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.Navy

@Composable
internal fun CustomerProfile(name: String, phone: String, onLanguage: () -> Unit, onLogout: () -> Unit, notifications: Boolean, onNotifications: (Boolean) -> Unit, notificationStatus: String?, onOpenPage: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var dialog by rememberSaveable { mutableStateOf("") }
    val language = copy("English", "اردو")
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(Navy, CircleShape), contentAlignment = Alignment.Center) {
                Text(name.take(1), color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Caption("+92 $phone")
            }
        }
    }
    Panel {
        SettingsRow(copy("Language", "زبان"), R.drawable.detail_language, onClick = { dialog = "language" }) {
            Caption(language)
            Chevron()
        }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Notifications", "اطلاعات"), R.drawable.detail_bell) {
            Switch(checked = notifications, onCheckedChange = onNotifications)
        }
        if (notificationStatus != null) Caption(copy(notificationStatus, "اطلاعات منسلک نہیں ہو سکیں۔ ایپ دوبارہ کھولنے پر کوشش ہو گی۔"))
        TextButton(onClick = {
            val intent = if (android.os.Build.VERSION.SDK_INT >= 26) android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            else android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
            try { context.startActivity(intent) } catch (_: android.content.ActivityNotFoundException) { /* The switch still controls in-app opt-in. */ }
        }) { Text(copy("Android notification settings", "اینڈرائیڈ اطلاعات کی سیٹنگز")) }

        HorizontalDivider(color = Divider)
        SettingsRow(copy("Help and support", "مدد اور معاونت"), R.drawable.detail_help, onClick = { onOpenPage("help") }) { Chevron() }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Privacy policy", "رازداری کی پالیسی"), R.drawable.detail_shield, onClick = { onOpenPage("privacy") }) { Chevron() }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("About", "تعارف"), R.drawable.payment_info, onClick = { onOpenPage("about") }) { Chevron() }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Log out", "لاگ آؤٹ"), R.drawable.detail_logout, destructive = true, onClick = { dialog = "logout" }) { Chevron() }
    }
    when (dialog) {
        "language" -> LanguagePicker(
            selectedUrdu = language == "اردو",
            onDismiss = { dialog = "" },
            onApply = { selected ->
                dialog = ""
                if (selected != (language == "اردو")) onLanguage()
            },
        )
        "logout" -> LogoutConfirmation({ dialog = "" }) {
            dialog = ""
            onLogout()
        }
    }
}

@Composable
private fun SettingsRow(title: String, icon: Int, destructive: Boolean = false, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    val color = if (destructive) Destructive else Blue
    val background = if (destructive) DestructiveBackground else Blue.copy(alpha = 0.08f)
    Row(
        Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Badge(icon, color, background)
        Text(title, Modifier.weight(1f), color = if (destructive) color else Navy, style = MaterialTheme.typography.titleMedium)
        trailing()
    }
}
