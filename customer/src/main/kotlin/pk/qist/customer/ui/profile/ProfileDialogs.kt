package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.components.CustomerBottomSheet
import pk.qist.customer.ui.components.CustomerDialog
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.BlueBackground
import pk.qist.customer.ui.theme.InputBorder
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePicker(selectedUrdu: Boolean, onDismiss: () -> Unit, onApply: (Boolean) -> Unit) {
    var urdu by rememberSaveable { mutableStateOf(selectedUrdu) }
    CustomerBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(QistSpacing.Section),
        ) {
            Text(copy("Choose language", "زبان منتخب کریں"), style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
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
        shape = QistShapes.Option,
        color = if (selected) BlueBackground else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Blue else InputBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Caption(subtitle)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
internal fun LogoutConfirmation(onDismiss: () -> Unit, onLogout: () -> Unit) {
    CustomerDialog(
        title = copy("Log out?", "لاگ آؤٹ کریں؟"),
        message = copy("You will need your phone number and password to sign in again.", "دوبارہ سائن ان کرنے کے لیے فون نمبر اور پاس ورڈ درکار ہوگا۔"),
        icon = R.drawable.detail_logout, destructive = true,
        confirmLabel = copy("Log out", "لاگ آؤٹ"), dismissLabel = copy("Cancel", "منسوخ"),
        onConfirm = onLogout, onDismiss = onDismiss,
    )
}
