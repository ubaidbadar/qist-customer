package pk.qist.device.enrolment

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.PersistableBundle
import androidx.core.content.IntentCompat

/** The key the portal puts the one-time token under, inside the QR's admin-extras bundle. */
private const val ENROL_TOKEN_KEY = "enrollToken"

/** The enrolment token carried by a provisioning intent, or null when there is none. */
fun Intent.enrolToken(): String? = IntentCompat.getParcelableExtra(
    this,
    DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
    PersistableBundle::class.java
)?.getString(ENROL_TOKEN_KEY)?.takeIf { it.isNotBlank() }
