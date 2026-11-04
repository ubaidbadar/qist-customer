package pk.qist.device.checkin

import androidx.datastore.core.DataStore
import pk.qist.device.data.DeviceState
import pk.qist.device.data.LockState
import pk.qist.device.deviceadmin.DevicePolicy
import pk.qist.device.deviceadmin.PolicyResult

/**
 * Carries out lock, unlock and release on the phone, keeping the stored lock state in step. The state is saved
 * before locking so the lock screen has its content the moment it opens.
 */
class CommandRunner(private val store: DataStore<DeviceState>, private val policy: DevicePolicy) {
    suspend fun run(command: Command): PolicyResult = when (command) {
        is Command.Lock -> lock()
        is Command.Unlock -> unlock()
        is Command.Release -> policy.release().also { if (it == PolicyResult.Applied) setLockState(LockState.UNLOCKED) }
        is Command.Unsupported -> PolicyResult.Failed("unsupported command: ${command.type}")
    }

    suspend fun lock(): PolicyResult {
        setLockState(LockState.LOCKED)
        val result = policy.lock()
        if (result is PolicyResult.Failed) setLockState(LockState.UNLOCKED)
        return result
    }

    suspend fun unlock(): PolicyResult {
        setLockState(LockState.UNLOCKED)
        return policy.unlock()
    }

    private suspend fun setLockState(lockState: LockState) {
        store.updateData { it.copy(lockState = lockState) }
    }
}
