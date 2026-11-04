package pk.qist.device.enrolment

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pk.qist.device.container

/**
 * Android 10+ setup hands control to the app once it is Device Owner, with the QR's admin extras. The token is
 * stored and enrolment queued; setup continues without waiting for the network.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class PolicyComplianceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.enrolToken()
        lifecycleScope.launch {
            if (token != null) container.enroller.accept(token)
            setResult(RESULT_OK)
            finish()
        }
    }
}
