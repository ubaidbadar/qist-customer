package pk.qist.device.enrolment

sealed interface EnrolOutcome {
    /** The credential is stored; [checkInSeconds] is how often the server wants to hear from the phone. */
    data class Enrolled(val checkInSeconds: Long) : EnrolOutcome

    /** The server refused the QR token: expired or already used. Retrying cannot help. */
    data object Rejected : EnrolOutcome

    /** No network or a server error: try again later with the same token. */
    data object RetryLater : EnrolOutcome

    /** No QR token waiting (already enrolled, or never provisioned). */
    data object NothingToDo : EnrolOutcome
}
