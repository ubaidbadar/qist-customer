package pk.qist.customer.data

import android.content.Context
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class CustomerPreferenceSnapshot(val accepted: Boolean, val reminders: Boolean, val hasReminderPreference: Boolean)

internal class CustomerPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val privacy by lazy { appContext.getSharedPreferences("customer_privacy", Context.MODE_PRIVATE) }
    private val notifications by lazy { appContext.getSharedPreferences("customer_notifications", Context.MODE_PRIVATE) }

    val hasAcceptedPrivacy: Boolean
        get() = privacy.getBoolean("accepted_live_v1", false)

    val notificationsEnabled: Boolean
        get() = notifications.getBoolean("enabled", false)

    val hasNotificationPreference: Boolean
        get() = notifications.contains("enabled")

    suspend fun load(): CustomerPreferenceSnapshot = withContext(Dispatchers.IO) {
        CustomerPreferenceSnapshot(hasAcceptedPrivacy, notificationsEnabled, hasNotificationPreference)
    }

    fun acceptPrivacy() {
        privacy.edit().putBoolean("accepted_live_v1", true).apply()
    }

    fun setNotifications(enabled: Boolean) {
        notifications.edit().putBoolean("enabled", enabled).apply()
    }

    private fun noticeKey(phone: String) = "read_" + MessageDigest.getInstance("SHA-256")
        .digest(phone.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun readNotices(phone: String): Set<String> = notifications.getStringSet(noticeKey(phone), emptySet()).orEmpty().toSet()

    fun saveReadNotices(phone: String, ids: Set<String>) {
        notifications.edit().putStringSet(noticeKey(phone), ids.toList().takeLast(512).toSet()).apply()
    }
}
