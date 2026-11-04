package pk.qist.device.data

import kotlinx.serialization.Serializable

@Serializable
data class CheckInRequest(val appVersion: String, val lockState: LockState, val pushToken: String? = null)
