package pk.qist.customer.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
            Box(Modifier.size(52.dp).background(Navy, CircleShape), contentAlignment = Alignment.Center) {
                Text(data.name.take(1), color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Column { Text(data.name, style = MaterialTheme.typography.titleLarge); Caption(data.phone) }
        }
    }
    data.plan?.let { plan -> Panel {
        Caption(copy("Your retailer", "آپ کا دکاندار"))
        Text(plan.retailer, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(QistSpacing.Item)); RetailerContactActions(plan.retailerPhone)
    } }
    Panel {
        ProfileSettingRow(copy("Language", "زبان"), R.drawable.detail_language, { dialog = ProfileDialog.Language }) { Caption(language); Chevron() }
        HorizontalDivider(color = Divider)
        ProfileSettingRow(copy("In-app reminders", "ایپ میں یاد دہانیاں"), R.drawable.detail_bell) {
            Switch(checked = reminders, onCheckedChange = onReminders)
        }
        HorizontalDivider(color = Divider)
        ProfileSettingRow(copy("Help and support", "مدد اور معاونت"), R.drawable.detail_help, { onPage(ProfilePage.Help) }) { Chevron() }
        ProfileSettingRow(copy("Privacy policy", "رازداری کی پالیسی"), R.drawable.detail_shield, { onPage(ProfilePage.Privacy) }) { Chevron() }
        TextButton(onClick = { dialog = ProfileDialog.Logout }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(copy("Log out", "لاگ آؤٹ"), color = Destructive) }
    }
    when (dialog) {
        ProfileDialog.Language -> LanguagePicker(language == "اردو", { dialog = null }) { selected -> dialog = null; if (selected != (language == "اردو")) onLanguage() }
        ProfileDialog.Logout -> LogoutConfirmation({ dialog = null }) { dialog = null; onLogout() }
        null -> Unit
    }
}

@Composable
private fun ProfileSettingRow(title: String, icon: Int, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        Badge(icon, Blue, Blue.copy(alpha = 0.08f))
        Text(title, Modifier.weight(1f), color = Navy, style = MaterialTheme.typography.titleMedium)
        trailing()
    }
}
