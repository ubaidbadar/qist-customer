package pk.qist.device.deviceadmin

/** Whether a Device Owner policy change took effect. [Failed.reason] is short and safe to send to the server. */
sealed interface PolicyResult {
    data object Applied : PolicyResult

    data class Failed(val reason: String) : PolicyResult
}
