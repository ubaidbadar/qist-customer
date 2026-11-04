package pk.qist.device.data

import kotlinx.serialization.Serializable

/** The server's answer to a successful enrolment. [token] is a secret and never appears in logs. */
@Serializable
data class DeviceCredential(val deviceId: String, val token: String, val checkInSeconds: Long) {
    override fun toString() = "DeviceCredential(deviceId=$deviceId, checkInSeconds=$checkInSeconds)"
}
