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
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.Badge
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Chevron
import pk.qist.customer.ui.components.DetailDialog
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Divider
import pk.qist.customer.ui.theme.Navy

@Composable
internal fun CustomerProfile(data: DashboardSample, phone: String, onLanguage: () -> Unit, onLogout: () -> Unit, notifications: Boolean, onNotifications: (Boolean) -> Unit, onOpenPage: (String) -> Unit) {
    var dialog by rememberSaveable { mutableStateOf("") }
    val language = copy("English", "اردو")
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(Navy, CircleShape), contentAlignment = Alignment.Center) {
                Text(data.name.take(1), color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(data.name, style = MaterialTheme.typography.titleLarge)
                Caption("+92 $phone")
            }
        }
    }
    Panel {
        Caption(copy("Your retailer", "آپ کا دکاندار"))
        Text(data.retailer, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { dialog = "contact" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(copy("Call", "کال کریں"))
            }
            OutlinedButton(onClick = { dialog = "contact" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(copy("WhatsApp", "واٹس ایپ"), color = Navy)
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
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Help and support", "مدد اور معاونت"), R.drawable.detail_help, onClick = { onOpenPage("help") }) { Chevron() }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Privacy policy", "رازداری کی پالیسی"), R.drawable.detail_shield, onClick = { onOpenPage("privacy") }) { Chevron() }
        HorizontalDivider(color = Divider)
        SettingsRow(copy("Log out", "لاگ آؤٹ"), R.drawable.detail_logout, destructive = true, onClick = { dialog = "logout" }) { Chevron() }
    }
    when (dialog) {
        "contact" -> DetailDialog(copy("Contact retailer", "دکاندار سے رابطہ"), copy("This demo has no retailer phone number.", "ڈیمو میں دکاندار کا نمبر موجود نہیں۔")) { dialog = "" }
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
    val color = if (destructive) Color(0xFFAA381D) else Blue
    val background = if (destructive) Color(0xFFFFE8E0) else Blue.copy(alpha = 0.08f)
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
