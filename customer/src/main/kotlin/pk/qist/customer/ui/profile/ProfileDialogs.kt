package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import pk.qist.customer.R
import pk.qist.customer.ui.components.Caption
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePicker(selectedUrdu: Boolean, onDismiss: () -> Unit, onApply: (Boolean) -> Unit) {
    var urdu by rememberSaveable { mutableStateOf(selectedUrdu) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface,
        shape = QistShapes.Sheet,
        scrimColor = SheetScrim,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = QistSpacing.AuthScreen, end = QistSpacing.AuthScreen, bottom = QistSpacing.AuthScreen),
            verticalArrangement = Arrangement.spacedBy(QistSpacing.Section),
        ) {
            Text(copy("Choose language", "زبان منتخب کریں"), style = MaterialTheme.typography.headlineSmall)
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
        color = if (selected) BlueBackground else CardSurface,
        border = BorderStroke(if (selected) QistSize.FocusBorder else QistSize.Border, if (selected) Blue else InputBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(QistSpacing.Section),
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
    val destructive = Destructive
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = QistShapes.Dialog, color = CardSurface) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(QistSpacing.AuthScreen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(QistSpacing.Section),
            ) {
                Box(Modifier.size(QistSize.HeroBadge).background(DestructiveBackground, QistShapes.Pill), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.detail_logout), contentDescription = null, tint = destructive, modifier = Modifier.size(QistSize.IconMax))
                }
                Text(copy("Log out?", "لاگ آؤٹ کریں؟"), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    copy("You will need your phone number and password to sign in again.", "دوبارہ سائن ان کرنے کے لیے فون نمبر اور پاس ورڈ درکار ہوگا۔"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = QistSize.TouchTarget), contentPadding = PaddingValues(horizontal = QistSpacing.Small)) {
                        Text(copy("Cancel", "منسوخ"))
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.weight(1f).heightIn(min = QistSize.TouchTarget),
                        colors = ButtonDefaults.buttonColors(containerColor = destructive, contentColor = CardSurface),
                        contentPadding = PaddingValues(horizontal = QistSpacing.Small),
                    ) { Text(copy("Log out", "لاگ آؤٹ")) }
                }
            }
        }
    }
}
