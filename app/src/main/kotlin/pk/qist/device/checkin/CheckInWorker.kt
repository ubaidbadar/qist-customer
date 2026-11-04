package pk.qist.device.checkin

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pk.qist.device.container

/** Runs one [CheckIn]; WorkManager retries with back-off when the server cannot be reached. */
class CheckInWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (applicationContext.container.checkIn.run()) {
        CheckInOutcome.Done, CheckInOutcome.NotEnrolled, CheckInOutcome.StoodDown, CheckInOutcome.Released ->
            Result.success()

        CheckInOutcome.RetryLater -> Result.retry()
    }
}
