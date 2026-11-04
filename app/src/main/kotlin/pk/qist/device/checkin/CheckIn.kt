package pk.qist.device.checkin

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pk.qist.device.AppInfo
import pk.qist.device.WorkScheduler
import pk.qist.device.data.AckRequest
import pk.qist.device.data.ApiResult
import pk.qist.device.data.CheckInRequest
import pk.qist.device.data.CheckInResponse
import pk.qist.device.data.DeviceApi
import pk.qist.device.data.DeviceState
import pk.qist.device.data.Enrolment
import pk.qist.device.data.LockState
import pk.qist.device.deviceadmin.DevicePolicy
import pk.qist.device.deviceadmin.PolicyResult

/**
 * One check-in: report in, save the lock screen, carry out each command in order and acknowledge it. A command
 * whose acknowledgement does not get through stays "delivered" on the server and comes back next time; carrying
 * it out again is harmless, so nothing has to be queued on the phone.
 */
class CheckIn(
    private val api: DeviceApi,
    private val store: DataStore<DeviceState>,
    private val policy: DevicePolicy,
    private val runner: CommandRunner,
    private val scheduler: WorkScheduler,
    private val appInfo: AppInfo
) {
    /** The periodic and the on-demand check-in may start together; they take turns. */
    private val mutex = Mutex()

    suspend fun run(): CheckInOutcome = mutex.withLock {
        val state = store.data.first()
        val enrolled = when (val enrolment = state.enrolment) {
            is Enrolment.Enrolled -> enrolment
            is Enrolment.Releasing -> return confirmRelease(enrolment)
            else -> return CheckInOutcome.NotEnrolled
        }
        when (val result = api.checkIn(enrolled.credential, CheckInRequest(appInfo.appVersion, state.lockState, state.pushToken.takeIf { it.isNotBlank() }))) {
            is ApiResult.Success -> apply(enrolled.credential, state, result.value)
            ApiResult.Unauthorized -> standDown()
            ApiResult.NetworkError, is ApiResult.ServerError -> CheckInOutcome.RetryLater
        }
    }

    private suspend fun apply(credential: String, before: DeviceState, response: CheckInResponse): CheckInOutcome {
        store.updateData { it.copy(lockScreen = response.lockScreen, checkInSeconds = response.checkInSeconds) }
        if (response.checkInSeconds != before.checkInSeconds) scheduler.scheduleCheckIns(response.checkInSeconds)
        policy.protect()

        if (response.commands.isEmpty()) {
            reconcile(before.lockState, response.lockState)
            return CheckInOutcome.Done
        }
        for (command in response.commands.map { it.toCommand() }) {
            val outcome = carryOut(credential, command)
            if (outcome != CheckInOutcome.Done) return outcome
        }
        return CheckInOutcome.Done
    }

    private suspend fun carryOut(credential: String, command: Command): CheckInOutcome {
        val result = runner.run(command)
        if (command is Command.Release && result == PolicyResult.Applied) {
            val enrolled = store.data.first().enrolment as? Enrolment.Enrolled ?: return CheckInOutcome.NotEnrolled
            val releasing = Enrolment.Releasing(enrolled.deviceId, credential, command.id)
            store.updateData { it.copy(enrolment = releasing) }
            return confirmRelease(releasing)
        }
        val ack = when (result) {
            PolicyResult.Applied -> AckRequest(AckRequest.Outcome.APPLIED)
            is PolicyResult.Failed -> AckRequest(AckRequest.Outcome.FAILED, result.reason)
        }
        return when (api.acknowledge(credential, command.id, ack)) {
            ApiResult.Unauthorized -> standDown()

            is ApiResult.Success -> CheckInOutcome.Done

            // Not confirmed: the server delivers the command again next time.
            ApiResult.NetworkError, is ApiResult.ServerError -> CheckInOutcome.Done
        }
    }

    /**
     * Tells the server the release is done, then finishes it. A 409 means the server already has the confirmation
     * (an earlier attempt got through), and a 401 means it no longer knows the phone; either way the shop already
     * decided to release, so the phone is handed back. Anything else is retried on the next run.
     */
    private suspend fun confirmRelease(releasing: Enrolment.Releasing): CheckInOutcome {
        val ack = AckRequest(AckRequest.Outcome.APPLIED)
        return when (val result = api.acknowledge(releasing.credential, releasing.commandId, ack)) {
            is ApiResult.Success, ApiResult.Unauthorized -> finishRelease()
            is ApiResult.ServerError -> if (result.status == CONFLICT) finishRelease() else CheckInOutcome.RetryLater
            ApiResult.NetworkError -> CheckInOutcome.RetryLater
        }
    }

    /**
     * With no commands waiting, the server's lock state is the truth (it only changes when the phone confirms a
     * command). This repairs a phone that lost its local state.
     */
    private suspend fun reconcile(local: LockState, server: LockState) {
        if (local == server) return
        when (server) {
            LockState.LOCKED -> runner.lock()
            LockState.UNLOCKED -> runner.unlock()
        }
    }

    /**
     * The server no longer knows this phone (enrolment revoked). Never leave a customer locked by a phone the shop
     * no longer manages; but keep Device Owner and the anti-reset protections, because only an authenticated
     * release may hand the phone back for good.
     */
    private suspend fun standDown(): CheckInOutcome {
        runner.unlock()
        store.updateData { it.copy(enrolment = Enrolment.Revoked) }
        scheduler.cancelCheckIns()
        return CheckInOutcome.StoodDown
    }

    /**
     * The server has the release confirmation: forget the credential, give up Device Owner, and only then stop
     * check-ins. Cancelling check-ins stops the very job running this code, so it must come last.
     */
    private suspend fun finishRelease(): CheckInOutcome {
        store.updateData { DeviceState(enrolment = Enrolment.Released) }
        policy.relinquish()
        scheduler.cancelCheckIns()
        return CheckInOutcome.Released
    }

    private companion object {
        const val CONFLICT = 409
    }
}
