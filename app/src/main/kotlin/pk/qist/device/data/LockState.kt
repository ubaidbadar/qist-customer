package pk.qist.device.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LockState {
    @SerialName("unlocked")
    UNLOCKED,

    @SerialName("locked")
    LOCKED
}
