package pk.qist.device.deviceadmin

/**
 * What the app can do to the phone as Device Owner. An interface so the enrolment and check-in logic can be
 * tested on the JVM with a fake; [DpmDevicePolicy] is the real one. Every call is safe to repeat.
 */
interface DevicePolicy {
    /** Blocks factory reset, safe boot, new users, USB debugging and uninstalling this app. */
    fun protect(): PolicyResult

    /** Pins the lock screen (kiosk mode, also as the Home screen so it survives a reboot) and shows it. */
    fun lock(): PolicyResult

    /** Removes the lock screen. The protections stay. */
    fun unlock(): PolicyResult

    /** Paid off: removes the lock and every protection. */
    fun release(): PolicyResult

    /** Gives up Device Owner for good. Only after [release] and after the server has been told. */
    fun relinquish(): PolicyResult
}
