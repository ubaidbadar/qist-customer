package pk.qist.device.enrolment

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pk.qist.device.container

/** Runs [Enroller.enrol] in the background; WorkManager retries it with back-off while the network is down. */
class EnrolWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (applicationContext.container.enroller.enrol()) {
        is EnrolOutcome.Enrolled, EnrolOutcome.Rejected, EnrolOutcome.NothingToDo -> Result.success()
        EnrolOutcome.RetryLater -> Result.retry()
    }
}
