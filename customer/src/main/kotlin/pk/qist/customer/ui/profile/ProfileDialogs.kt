package pk.qist.customer.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import pk.qist.customer.R
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.components.ProfileCaption
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Navy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePicker(selectedUrdu: Boolean, onDismiss: () -> Unit, onApply: (Boolean) -> Unit) {
    var urdu by rememberSaveable { mutableStateOf(selectedUrdu) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        scrimColor = Navy.copy(alpha = 0.60f),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(copy("Choose language", "زبان منتخب کریں"), style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFEDF0FF) else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Blue else Color(0xFFBFCBE4)),
    ) {
        Row(
            Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                ProfileCaption(subtitle)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
internal fun LogoutConfirmation(onDismiss: () -> Unit, onLogout: () -> Unit) {
    val destructive = Color(0xFFAA381D)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(64.dp).background(Color(0xFFFFE8E0), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.detail_logout), contentDescription = null, tint = destructive, modifier = Modifier.size(30.dp))
                }
                Text(copy("Log out?", "لاگ آؤٹ کریں؟"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    copy("You will need your phone number and a code to sign in again.", "دوبارہ سائن ان کرنے کے لیے فون نمبر اور کوڈ درکار ہوگا۔"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(copy("Cancel", "منسوخ"))
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = destructive, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text(copy("Log out", "لاگ آؤٹ")) }
                }
            }
        }
    }
}
