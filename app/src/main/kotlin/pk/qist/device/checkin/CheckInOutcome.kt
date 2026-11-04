package pk.qist.device.checkin

sealed interface CheckInOutcome {
    /** Checked in and carried out every command (or left a failed acknowledgement for the next check-in). */
    data object Done : CheckInOutcome

    /** No network or a server error; nothing changed on the phone. */
    data object RetryLater : CheckInOutcome

    /** Not enrolled (yet, or any more): there is nobody to check in with. */
    data object NotEnrolled : CheckInOutcome

    /** The server no longer recognises the phone: it was unlocked and check-ins stopped. */
    data object StoodDown : CheckInOutcome

    /** Paid off: protections removed, Device Owner given up, check-ins stopped. */
    data object Released : CheckInOutcome
}
