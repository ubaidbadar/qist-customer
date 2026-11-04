package pk.qist.device.lock

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pk.qist.device.WorkScheduler
import pk.qist.device.container
import pk.qist.device.data.DeviceState

/** The lock screen's one source of truth: the stored state, mapped to what the screen shows. */
class LockViewModel(store: DataStore<DeviceState>, private val scheduler: WorkScheduler) : ViewModel() {
    val uiState: StateFlow<LockUiState> = store.data
        .map { it.toLockUiState() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LockUiState.Loading)

    /**
     * Asks the server now whether the phone may unlock. A locked customer cannot open anything else, so waking the
     * screen after paying at the counter is how they get unlocked without waiting for the next scheduled check-in.
     */
    fun refresh() = scheduler.checkInNow()

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).container
                LockViewModel(container.store, container.scheduler)
            }
        }
    }
}
