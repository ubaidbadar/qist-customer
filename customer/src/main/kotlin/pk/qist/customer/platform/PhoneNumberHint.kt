package pk.qist.customer.platform

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import pk.qist.customer.ui.auth.normalizePhone
import pk.qist.customer.ui.localization.copy

/** User selects a SIM hint; never needs phone/SMS/contact permissions and never submits it automatically. */
@Composable
internal fun PhoneNumberHint(empty: Boolean, enabled: Boolean, showAction: Boolean = true, onNumber: (String) -> Unit) {
    val activity = LocalActivity.current ?: return
    val currentOnNumber by rememberUpdatedState(onNumber)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentEmpty by rememberUpdatedState(empty)
    var offered by rememberSaveable { mutableStateOf(false) }
    var available by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { active = true; onDispose { active = false } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        pending = false
        if (currentEnabled && result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                val number = normalizePhone(Identity.getSignInClient(activity).getPhoneNumberFromIntent(result.data))
                if (number.matches(Regex("3[0-9]{9}"))) currentOnNumber(number)
            } catch (_: ApiException) { available = false }
        }
    }
    fun requestHint(automatic: Boolean = false) {
        if (pending) return
        offered = true; pending = true
        Identity.getSignInClient(activity).getPhoneNumberHintIntent(GetPhoneNumberHintIntentRequest.builder().build())
            .addOnSuccessListener { intent ->
                if (!active || !currentEnabled || (automatic && !currentEmpty)) { pending = false; return@addOnSuccessListener }
                try { launcher.launch(IntentSenderRequest.Builder(intent).build()) }
                catch (_: android.content.ActivityNotFoundException) { pending = false; available = false }
                catch (_: android.content.IntentSender.SendIntentException) { pending = false; available = false }
            }.addOnFailureListener { pending = false; available = false }
    }
    LaunchedEffect(empty, enabled) { if (empty && enabled && !offered) requestHint(automatic = true) }
    if (available && showAction) TextButton(onClick = { requestHint() }, enabled = enabled && !pending) {
        Text(copy("Use phone number from SIM", "سم کا فون نمبر استعمال کریں"))
    }
}
