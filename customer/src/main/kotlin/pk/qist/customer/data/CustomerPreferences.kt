package pk.qist.customer.data

import android.content.Context

internal class CustomerPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val privacy by lazy { appContext.getSharedPreferences("customer_privacy", Context.MODE_PRIVATE) }
    private val notifications by lazy { appContext.getSharedPreferences("customer_notifications", Context.MODE_PRIVATE) }

    val hasAcceptedPrivacy: Boolean
        get() = privacy.getBoolean("accepted_api_v1", false)

    val notificationsEnabled: Boolean
        get() = notifications.getBoolean("enabled", false)

    val hasNotificationPreference: Boolean
        get() = notifications.contains("enabled")

    fun acceptPrivacy() {
        privacy.edit().putBoolean("accepted_api_v1", true).apply()
    }

    fun setNotifications(enabled: Boolean) {
        notifications.edit().putBoolean("enabled", enabled).apply()
    }
}
