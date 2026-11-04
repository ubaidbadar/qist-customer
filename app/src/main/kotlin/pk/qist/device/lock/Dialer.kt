package pk.qist.device.lock

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the phone's dialer, with [number] filled in when given. Dialling (not calling) needs no permission, and
 * the dialer is allowed while locked, so emergency numbers always work from it.
 */
fun Context.openDialer(number: String? = null) {
    val intent = Intent(Intent.ACTION_DIAL).apply { if (number != null) data = Uri.fromParts("tel", number, null) }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A phone without a dialer (some tablets) cannot call; the button simply does nothing.
    }
}
