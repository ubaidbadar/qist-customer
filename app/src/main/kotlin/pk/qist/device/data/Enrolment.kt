package pk.qist.device.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where this phone stands with the server. The secrets inside never show up in `toString()`. */
@Serializable
sealed interface Enrolment {
    /** Installed but not provisioned by a shop QR (for example, a developer install). */
    @Serializable
    @SerialName("not_provisioned")
    data object NotProvisioned : Enrolment

    /** Provisioned from a QR; the one-time token still has to be redeemed. */
    @Serializable
    @SerialName("pending")
    data class Pending(val enrolToken: String) : Enrolment {
        override fun toString() = "Pending"
    }

    @Serializable
    @SerialName("enrolled")
    data class Enrolled(val deviceId: String, val credential: String) : Enrolment {
        override fun toString() = "Enrolled(deviceId=$deviceId)"
    }

    /**
     * Paid off: protections already removed, waiting for the server to confirm [commandId]. Saved before telling the
     * server so an interrupted release is finished on the next run instead of being protected again.
     */
    @Serializable
    @SerialName("releasing")
    data class Releasing(val deviceId: String, val credential: String, val commandId: String) : Enrolment {
        override fun toString() = "Releasing(deviceId=$deviceId)"
    }

    /** The server refused the QR token (expired or already used). The shop has to set the phone up again. */
    @Serializable
    @SerialName("rejected")
    data object Rejected : Enrolment

    /** The server revoked this phone's credential. The app no longer locks or checks in. */
    @Serializable
    @SerialName("revoked")
    data object Revoked : Enrolment

    /** Paid off: every protection removed and Device Owner given up. The phone is the customer's alone. */
    @Serializable
    @SerialName("released")
    data object Released : Enrolment
}
