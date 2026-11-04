package pk.qist.device.push

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pk.qist.device.container
import pk.qist.device.data.Enrolment

/** Persist refreshed FCM registration tokens so check-ins can upload them to the authenticated API. */
fun Context.recordPushToken(token: String) {
    if (token.isBlank()) return
    val app = applicationContext
    val container = app.container
    container.appScope.launch {
        val current = container.store.data.first()
        if (current.pushToken == token) return@launch
        container.store.updateData { it.copy(pushToken = token) }
        if (current.enrolment is Enrolment.Enrolled) container.scheduler.checkInNow()
    }
}
