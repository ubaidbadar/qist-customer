package pk.qist.device.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AckRequest(val result: Outcome, val detail: String? = null) {
    @Serializable
    enum class Outcome {
        @SerialName("applied")
        APPLIED,

        @SerialName("failed")
        FAILED
    }
}
