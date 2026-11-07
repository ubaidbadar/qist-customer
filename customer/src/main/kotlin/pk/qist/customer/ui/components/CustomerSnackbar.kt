package pk.qist.customer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pk.qist.customer.R
import pk.qist.customer.ui.theme.*

private data class AccountSnackbar(
    override val message: String,
    override val actionLabel: String?,
    val error: Boolean,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
    override val withDismissAction: Boolean = false,
) : SnackbarVisuals

internal suspend fun SnackbarHostState.accountMessage(message: String, action: String? = null, error: Boolean = false): SnackbarResult =
    showSnackbar(AccountSnackbar(message, action, error))

@Composable
internal fun CustomerSnackbarHost(state: SnackbarHostState) {
    SnackbarHost(state) { data ->
        val error = (data.visuals as? AccountSnackbar)?.error == true
        Snackbar(shape = QistShapes.Input, containerColor = Navy, contentColor = androidx.compose.ui.graphics.Color.White,
            action = {
                data.visuals.actionLabel?.let { label -> TextButton(onClick = data::performAction) { Text(label, color = Cyan) } }
            }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(if (error) R.drawable.state_error else R.drawable.detail_check), null,
                    Modifier.size(20.dp), tint = if (error) Coral else Cyan)
                Text(data.visuals.message, Modifier.weight(1f))
            }
        }
    }
}
