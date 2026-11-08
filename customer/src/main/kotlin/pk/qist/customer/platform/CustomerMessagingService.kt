package pk.qist.customer.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import pk.qist.customer.MainActivity
import pk.qist.customer.R
import pk.qist.customer.data.CustomerPreferences

internal object CustomerPushSignals {
    val revision = MutableStateFlow(0L)
    val token = MutableStateFlow<String?>(null)
    val openInbox = MutableStateFlow(false)
    fun changed() { revision.update { it + 1 } }
}

class CustomerMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { CustomerPushSignals.token.value = token }
    override fun onDeletedMessages() { CustomerPushSignals.changed() }
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["action"] != "customer_changed") return
        CustomerPushSignals.changed()
        val preferences = CustomerPreferences(this)
        if (!preferences.notificationsEnabled || !preferences.customerActive) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Qist account updates", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(this, MainActivity::class.java).putExtra("open_customer_notifications", true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome).setContentTitle("Qist")
            .setContentText("There is an update to your account. Open Qist to view it.")
            .setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(pending).build()
        try { manager.notify(message.data["eventId"]?.hashCode() ?: 1, notification) } catch (_: SecurityException) { /* Permission may have been revoked. */ }
    }
    companion object { const val CHANNEL = "qist_customer_updates" }
}
