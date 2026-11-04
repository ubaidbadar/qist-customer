package pk.qist.device.data

import kotlinx.serialization.Serializable

@Serializable
data class EnrollRequest(
    val token: String,
    val appVersion: String,
    val androidVersion: String,
    val pushToken: String? = null,
    val platform: String = "android"
) {
    override fun toString() = "EnrollRequest(appVersion=$appVersion, androidVersion=$androidVersion)"
}
