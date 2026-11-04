package pk.qist.device.data

import kotlinx.serialization.Serializable

@Serializable
data class CheckInResponse(
    val lockState: LockState,
    val lockScreen: LockScreen,
    val commands: List<CommandDto>,
    val checkInSeconds: Long
)
