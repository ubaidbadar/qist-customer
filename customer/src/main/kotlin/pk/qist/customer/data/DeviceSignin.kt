package pk.qist.customer.data

import java.security.SecureRandom
import okio.ByteString.Companion.toByteString

internal data class DeviceChallenge(val requestId: String, val expiresAt: String, val number: String? = null)
internal data class DeviceClaim(val state: String, val ticket: String? = null, val phone: String? = null, val name: String? = null) {
    override fun toString() = "DeviceClaim(state=$state)"
}
internal data class DeviceIdentity(val next: String, val phone: String? = null, val setupToken: String? = null) {
    override fun toString() = "DeviceIdentity(next=$next)"
}

/** A trusted local app can approve a backend challenge, never supply a customer session itself. */
internal interface LockerAuthenticator {
    suspend fun available(): Boolean
    suspend fun authenticate(requestId: String, nonce: String): Boolean
}

internal fun newSigninNonce(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.toByteString().base64Url().trimEnd('=')
