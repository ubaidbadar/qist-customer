package pk.qist.customer.documents

import android.net.Uri
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

internal data class DocumentsState(val data: CustomerDocuments? = null, val loading: Boolean = true,
    val busy: Boolean = false, val message: String? = null, val unavailable: Boolean = false)
internal class DocumentsViewModel(private val api: DocumentApi, private val uploads: PrivateUploads, private val contractId: String) : ViewModel() {
    private val mutable = MutableStateFlow(DocumentsState())
    val state = mutable.asStateFlow()
    private var refreshJob: kotlinx.coroutines.Job? = null
    init { refresh() }
    fun refresh() {
        if (mutable.value.busy || refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = mutable.value.data == null, message = null)
            try { mutable.value = mutable.value.copy(data = api.list(contractId), unavailable = false) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: IOException) { mutable.value = mutable.value.copy(message = if (error is pk.qist.customer.data.CustomerApiException) error.message else "Could not load documents. Check your connection and try again.", unavailable = error is pk.qist.customer.data.CustomerApiException && error.status == 404) }
            finally { mutable.value = mutable.value.copy(loading = false) }
        }
    }
    fun more() = action {
        val current = mutable.value.data ?: return@action
        val cursor = current.next ?: return@action
        val next = api.list(contractId, cursor)
        mutable.value = mutable.value.copy(data = current.copy(items = (current.items + next.items).distinctBy { it.id }, next = next.next))
    }
    fun enqueue(uri: Uri, category: String, requestId: String?, replacesId: String?, onQueued: () -> Unit) = action {
        val current = mutable.value.data ?: return@action
        uploads.enqueue(uri, contractId, category, current.maxBytes, requestId, replacesId)
        mutable.value = mutable.value.copy(message = "File queued securely. Uploading may wait for an internet connection.")
        onQueued()
    }
    fun remove(item: CustomerDocument) = action {
        api.remove(item)
        mutable.value = mutable.value.copy(data = mutable.value.data?.let { it.copy(items = it.items.filterNot { old -> old.id == item.id }) })
    }
    fun cancel(id: UUID) = action { uploads.cancel(id) }
    fun export(item: CustomerDocument, write: (okhttp3.ResponseBody) -> Unit) = action { api.export(item, write) }
    private fun action(block: suspend () -> Unit) {
        if (mutable.value.busy || refreshJob?.isActive == true) return
        mutable.value = mutable.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: IOException) { mutable.value = mutable.value.copy(message = if (error is pk.qist.customer.data.CustomerApiException || error is DocumentInputException) error.message else "Could not complete the document request. Check your connection or choose the file again.") }
            catch (_: java.security.GeneralSecurityException) { mutable.value = mutable.value.copy(message = "Secure file storage is unavailable. Please try again.") }
            catch (_: SecurityException) { mutable.value = mutable.value.copy(message = "File access was denied. Choose the file again.") }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }
}
