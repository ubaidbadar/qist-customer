package pk.qist.device.checkin

import pk.qist.device.data.CommandDto

/** A command from the server, in the app's own terms. */
sealed interface Command {
    val id: String

    data class Lock(override val id: String) : Command

    data class Unlock(override val id: String) : Command

    data class Release(override val id: String) : Command

    /** A type this version of the app does not know; it is acknowledged as failed so the server is not left waiting. */
    data class Unsupported(override val id: String, val type: String) : Command
}

fun CommandDto.toCommand(): Command = when (type) {
    "lock" -> Command.Lock(id)
    "unlock" -> Command.Unlock(id)
    "release" -> Command.Release(id)
    else -> Command.Unsupported(id, type)
}
