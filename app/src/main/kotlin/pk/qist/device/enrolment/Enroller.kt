package pk.qist.device.enrolment

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import pk.qist.device.AppInfo
import pk.qist.device.WorkScheduler
import pk.qist.device.data.ApiResult
import pk.qist.device.data.DeviceApi
import pk.qist.device.data.DeviceState
import pk.qist.device.data.EnrollRequest
import pk.qist.device.data.Enrolment
import pk.qist.device.deviceadmin.DevicePolicy

/** Turns the one-time QR token into the phone's long-lived credential. */
class Enroller(
    private val api: DeviceApi,
    private val store: DataStore<DeviceState>,
    private val policy: DevicePolicy,
    private val scheduler: WorkScheduler,
    private val appInfo: AppInfo
) {
    /**
     * Keeps the token from the provisioning QR and queues enrolment. Called from every provisioning callback, so it
     * is safe to repeat, and it never replaces a credential the phone already has.
     */
    suspend fun accept(enrolToken: String) {
        val state = store.updateData { current ->
            if (current.enrolment is Enrolment.Enrolled) {
                current
            } else {
                current.copy(
                    enrolment = Enrolment.Pending(enrolToken)
                )
            }
        }
        if (state.enrolment is Enrolment.Pending) scheduler.enrol()
    }

    /** Redeems the waiting token, if any. Protections go on only after the server has accepted the phone. */
    suspend fun enrol(): EnrolOutcome {
        val pending = store.data.first().enrolment as? Enrolment.Pending ?: return EnrolOutcome.NothingToDo
        val pushToken = store.data.first().pushToken.takeIf { it.isNotBlank() }
        val request = EnrollRequest(pending.enrolToken, appInfo.appVersion, appInfo.androidVersion, pushToken = pushToken)
        return when (val result = api.enroll(request)) {
            is ApiResult.Success -> {
                val credential = result.value
                store.updateData {
                    it.copy(
                        enrolment = Enrolment.Enrolled(credential.deviceId, credential.token),
                        checkInSeconds = credential.checkInSeconds
                    )
                }
                policy.protect()
                scheduler.scheduleCheckIns(credential.checkInSeconds)
                scheduler.checkInNow()
                EnrolOutcome.Enrolled(credential.checkInSeconds)
            }

            ApiResult.Unauthorized -> {
                store.updateData { it.copy(enrolment = Enrolment.Rejected) }
                EnrolOutcome.Rejected
            }

            ApiResult.NetworkError, is ApiResult.ServerError -> EnrolOutcome.RetryLater
        }
    }
}
