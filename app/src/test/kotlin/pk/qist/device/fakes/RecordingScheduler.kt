package pk.qist.device.fakes

import kotlinx.coroutines.CancellationException
import pk.qist.device.WorkScheduler

/**
 * Records what was asked for. With [cancelStopsCaller], cancelling check-ins also stops the caller, as WorkManager
 * does when the code that cancels is itself running inside a check-in job.
 */
class RecordingScheduler(private val cancelStopsCaller: Boolean = false) : WorkScheduler {
    val requests = mutableListOf<String>()

    override fun enrol() {
        requests += "enrol"
    }

    override fun scheduleCheckIns(seconds: Long) {
        requests += "every $seconds s"
    }

    override fun checkInNow() {
        requests += "check in now"
    }

    override fun cancelCheckIns() {
        requests += "cancel check-ins"
        if (cancelStopsCaller) throw CancellationException("check-in job cancelled")
    }
}
