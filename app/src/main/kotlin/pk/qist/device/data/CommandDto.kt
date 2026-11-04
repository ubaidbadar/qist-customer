package pk.qist.device.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A command as sent by the server. [type] stays a string so a newer server cannot break an older app. */
@Serializable
data class CommandDto(@SerialName("_id") val id: String, val type: String)
