package pk.qist.device.fakes

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pk.qist.device.data.DeviceState

/** A DataStore that keeps the state in memory, with the same one-writer-at-a-time rule as the real one. */
class InMemoryStore(initial: DeviceState = DeviceState()) : DataStore<DeviceState> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: StateFlow<DeviceState> = state

    val value: DeviceState get() = state.value

    override suspend fun updateData(transform: suspend (t: DeviceState) -> DeviceState): DeviceState =
        mutex.withLock { transform(state.value).also { state.value = it } }
}
