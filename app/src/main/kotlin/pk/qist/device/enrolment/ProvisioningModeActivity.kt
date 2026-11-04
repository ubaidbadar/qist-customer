package pk.qist.device.enrolment

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

/**
 * Android 10+ setup asks the app which kind of provisioning it wants; a financed phone is always a fully managed
 * device. Invisible: answers and closes at once.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = Intent().putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        )
        setResult(RESULT_OK, result)
        finish()
    }
}
