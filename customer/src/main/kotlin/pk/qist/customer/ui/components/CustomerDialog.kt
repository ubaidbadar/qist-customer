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

/** Earlier custom confirmation styling, shared across account actions. */
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
    val confirmAction: @Composable () -> Unit = {
        Button(onClick = onConfirm, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (destructive) Destructive else Blue, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 8.dp)) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(confirmLabel)
        }
    }
    val dismissAction: @Composable () -> Unit = {
        if (dismissLabel != null) OutlinedButton(onClick = onDismiss, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(dismissLabel)
        }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(
        dismissOnBackPress = !busy, dismissOnClickOutside = !busy,
    )) {
        Surface(Modifier.widthIn(max = 420.dp).fillMaxWidth().semantics { paneTitle = title },
            shape = QistShapes.Dialog, color = Color.White) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(QistSpacing.AuthScreen),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                ScreenBadge(icon, if (destructive) Destructive else Blue,
                    if (destructive) DestructiveBackground else BlueBackground, 64, iconSize = 30)
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                if (error != null) Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = Destructive, textAlign = TextAlign.Center)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // Preserve the earlier side-by-side buttons; stack when text or space needs it.
                    val stacked = maxWidth < 240.dp || LocalDensity.current.fontScale > 1.3f
                    if (stacked || dismissLabel == null) Column(verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                        confirmAction()
                        dismissAction()
                    } else Row(horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                        Box(Modifier.weight(1f)) { dismissAction() }
                        Box(Modifier.weight(1f)) { confirmAction() }
                    }
                }
            }
        }
    }
}

