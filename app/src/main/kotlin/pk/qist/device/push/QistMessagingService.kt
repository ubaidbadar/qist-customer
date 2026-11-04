package pk.qist.device.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pk.qist.device.container

/**
 * FCM is a wake-up hint, never a trusted command: the existing authenticated check-in
 * downloads pending instructions and reports the result to the server.
 */
class QistMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["action"] == "checkin") container.scheduler.checkInNow()
    }

    override fun onDeletedMessages() {
        container.scheduler.checkInNow()
    }

    override fun onNewToken(token: String) {
        recordPushToken(token)
    }
}
