package pk.qist.customer.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import pk.qist.customer.R
import pk.qist.customer.data.CustomerOverview
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

internal enum class ProfilePage { Help, Privacy }
private enum class ProfileDialog { Language, Logout }

@Composable
internal fun CustomerProfile(data: CustomerOverview, onLanguage: () -> Unit, onLogout: () -> Unit,
    reminders: Boolean, onReminders: (Boolean) -> Unit, busy: Boolean, onPage: (ProfilePage) -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }
    val language = copy("English", "اردو")
    Panel { Text(data.name, style = MaterialTheme.typography.titleLarge); Caption(data.phone) }
    data.plan?.let { plan -> Panel {
        Caption(copy("Your retailer", "آپ کا دکاندار"))
        Text(plan.retailer, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(QistSpacing.Item)); RetailerContactActions(plan.retailerPhone)
    } }
    Panel {
        TextButton(onClick = { dialog = ProfileDialog.Language }, modifier = Modifier.fillMaxWidth()) { Text(copy("Language", "زبان") + " · " + language) }
        HorizontalDivider(color = Divider)
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { Text(copy("In-app reminders", "ایپ میں یاد دہانیاں")); Caption(copy("Shown while you use Qist", "قسط استعمال کرتے وقت دکھائی جائیں گی")) }
            Switch(checked = reminders, onCheckedChange = onReminders)
        }
        HorizontalDivider(color = Divider)
        TextButton(onClick = { onPage(ProfilePage.Help) }, modifier = Modifier.fillMaxWidth()) { Text(copy("Help and support", "مدد اور معاونت")) }
        TextButton(onClick = { onPage(ProfilePage.Privacy) }, modifier = Modifier.fillMaxWidth()) { Text(copy("Privacy policy", "رازداری کی پالیسی")) }
        TextButton(onClick = { dialog = ProfileDialog.Logout }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(copy("Log out", "لاگ آؤٹ"), color = Destructive) }
    }
    when (dialog) {
        ProfileDialog.Language -> LanguagePicker(language == "اردو", { dialog = null }) { selected -> dialog = null; if (selected != (language == "اردو")) onLanguage() }
        ProfileDialog.Logout -> LogoutConfirmation({ dialog = null }) { dialog = null; onLogout() }
        null -> Unit
    }
}
