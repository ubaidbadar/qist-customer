package pk.qist.customer.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CustomerPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val privacy by lazy { appContext.getSharedPreferences("customer_privacy", Context.MODE_PRIVATE) }
    private val notifications by lazy { appContext.getSharedPreferences("customer_notifications", Context.MODE_PRIVATE) }

    private val notificationState = MutableStateFlow(notificationsEnabled)
    val notificationPreference = notificationState.asStateFlow()
    val customerActive: Boolean get() = notifications.getBoolean("active", false)
    fun setCustomerActive(active: Boolean) { notifications.edit().putBoolean("active", active).apply() }

    val urdu: Boolean get() = privacy.getBoolean("urdu", java.util.Locale.getDefault().language == "ur")
    fun setUrdu(urdu: Boolean) { privacy.edit().putBoolean("urdu", urdu).apply() }

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
        notificationState.value = enabled
    }
}
