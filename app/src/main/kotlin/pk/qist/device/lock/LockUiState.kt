package pk.qist.device.lock

import pk.qist.device.data.DeviceState
import pk.qist.device.data.LockScreen
import pk.qist.device.data.LockState

sealed interface LockUiState {
    /** The state file is still being read (a few milliseconds on a cold start). */
    data object Loading : LockUiState

    data class Locked(val screen: LockScreenUi) : LockUiState

    /** Not (or no longer) locked: the lock screen closes itself. */
    data object Unlocked : LockUiState
}

fun DeviceState.toLockUiState(): LockUiState = when (lockState) {
    LockState.LOCKED -> LockUiState.Locked(LockScreenFormatter.format(lockScreen ?: LockScreen()))
    LockState.UNLOCKED -> LockUiState.Unlocked
}
