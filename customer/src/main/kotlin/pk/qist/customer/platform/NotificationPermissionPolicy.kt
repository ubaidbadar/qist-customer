package pk.qist.customer.platform

internal enum class NotificationPermissionAction { Keep, Ask, TurnOff }

/** A resumed app must respect a permission revoked in Android Settings; only an explicit opt-in may ask. */
internal fun notificationPermissionAction(enabled: Boolean, explicitlyEnabled: Boolean, granted: Boolean): NotificationPermissionAction = when {
    !enabled || granted -> NotificationPermissionAction.Keep
    explicitlyEnabled -> NotificationPermissionAction.Ask
    else -> NotificationPermissionAction.TurnOff
}
