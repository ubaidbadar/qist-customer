package pk.qist.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

/**
 * Debug builds only: lets adb hand the app an enrolment token, standing in for the QR on an emulator, which cannot
 * run QR provisioning. `adb shell am broadcast -n pk.qist.device/.DebugEnrolReceiver --es token <token>`.
 * Guarded by the DUMP permission, which adb's shell holds and ordinary apps cannot get.
 */
class DebugEnrolReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getStringExtra("token") ?: return
        val pending = goAsync()
        context.container.appScope.launch {
            try {
                context.container.enroller.accept(token)
            } finally {
                pending.finish()
            }
        }
    }
}
