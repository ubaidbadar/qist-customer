package pk.qist.customer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pk.qist.customer.ui.theme.*

/** Shared customer confirmation card, matching the supplied delete-data dialog design. */
@Composable
internal fun CustomerDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: Int,
    dismissLabel: String? = null,
    destructive: Boolean = false,
    busy: Boolean = false,
    error: String? = null,
) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = !busy,
    )) {
        Surface(Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth().semantics { paneTitle = title },
            shape = QistShapes.Dialog, color = Color.White) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ScreenBadge(icon, if (destructive) Destructive else Blue,
                    if (destructive) DestructiveBackground else BlueBackground, 64)
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                if (error != null) Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = Destructive, textAlign = TextAlign.Center)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // Side-by-side actions match the reference; narrow screens and large text stack them.
                    val stacked = maxWidth < 240.dp || LocalDensity.current.fontScale > 1.3f
                    if (stacked || dismissLabel == null) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Pill(confirmLabel, onConfirm, busy = busy, destructive = destructive)
                        if (dismissLabel != null) SecondaryAction(dismissLabel, onDismiss, enabled = !busy)
                    } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { SecondaryAction(dismissLabel, onDismiss, enabled = !busy) }
                        Box(Modifier.weight(1f)) { Pill(confirmLabel, onConfirm, busy = busy, destructive = destructive) }
                    }
                }
            }
        }
    }
}

