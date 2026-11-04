package pk.qist.device

/** Background jobs the logic asks for. An interface so tests can record requests instead of running WorkManager. */
interface WorkScheduler {
    /** Redeem the stored QR token as soon as there is a network, retrying with back-off. */
    fun enrol()

    /** Check in every [seconds] (at least 15 minutes, WorkManager's floor), whenever there is a network. */
    fun scheduleCheckIns(seconds: Long)

    /** One extra check-in as soon as there is a network (app opened, phone rebooted, just enrolled). */
    fun checkInNow()

    /** Stop all check-ins: the phone is released or its enrolment was revoked. */
    fun cancelCheckIns()
}
