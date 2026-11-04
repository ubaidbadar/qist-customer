package pk.qist.device.deviceadmin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import pk.qist.device.container
import pk.qist.device.enrolment.enrolToken

/** The component Android makes Device Owner. Every policy call names it as the admin. */
class QistAdminReceiver : DeviceAdminReceiver() {
    /** Provisioning finished (all Android versions). The QR's token arrives here as well as in the compliance step. */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val token = intent.enrolToken() ?: return
        val pending = goAsync()
        context.container.appScope.launch {
            try {
                context.container.enroller.accept(token)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun component(context: Context) = ComponentName(context, QistAdminReceiver::class.java)
    }
}
