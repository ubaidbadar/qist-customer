package pk.qist.customer.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.IOException
import kotlinx.coroutines.flow.map
import pk.qist.customer.BuildConfig
import pk.qist.customer.R
import pk.qist.customer.data.CustomerSessionStore
import pk.qist.customer.data.KeystoreTextStore
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun DocumentsScreen(contractId: String, retailer: String, onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val model = viewModel(key = "documents:$contractId") {
        DocumentsViewModel(DocumentApi(CustomerSessionStore(KeystoreTextStore(context)), BuildConfig.CUSTOMER_API_URL), PrivateUploads(context), contractId)
    }
    val state by model.state.collectAsStateWithLifecycle()
    val workFlow = remember(contractId, context) { WorkManager.getInstance(context).getWorkInfosByTagFlow(planUploadTag(contractId)).map { rows -> rows.sortedBy { it.id.toString() } } }
    val snapshot by workFlow.collectAsStateWithLifecycle(initialValue = null)
    val work = snapshot.orEmpty()
    var observedCompleted by remember { mutableStateOf<Set<java.util.UUID>?>(null) }
    LaunchedEffect(snapshot) {
        if (snapshot != null) {
            val completed = work.filter { it.state == WorkInfo.State.SUCCEEDED }.map { it.id }.toSet()
            if (observedCompleted?.let { (completed - it).isNotEmpty() } == true) model.refresh()
            observedCompleted = completed
        }
    }
    var removing by remember { mutableStateOf<CustomerDocument?>(null) }
    var export by remember { mutableStateOf<CustomerDocument?>(null) }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val item = export
        if (uri != null && item != null) model.export(item) { body ->
            context.contentResolver.openOutputStream(uri, "w")?.use { output -> body.byteStream().use { input ->
                val buffer = ByteArray(32 * 1024); var total = 0L
                while (true) { val count = input.read(buffer); if (count < 0) break; total += count
                    if (total > 10 * 1024 * 1024) throw IOException("Document is too large.")
                    output.write(buffer, 0, count)
                }
            } } ?: throw IOException("Could not save the selected file.")
        }
        export = null
    }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = QistSpacing.Screen)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CustomerBackButton(onBack)
            Text("Documents", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = model::refresh, enabled = !state.busy && !state.loading) { Text("Refresh") }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "retailer") { Caption("Shared only with $retailer for this plan. Acceptance records staff review and does not independently verify identity.") }
            if (state.loading) item(key = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.message?.let { message -> item(key = "message") { AccountMessage(message) } }
            val data = state.data
            if (state.unavailable) item(key = "unavailable") { Panel { Text("Documents are not available yet. Your plan and payment features remain available.") } }
            if (data != null) {
                if (data.canUpload) item(key = "upload") { DocumentSubmission(data, state.busy, model::enqueue) }
                else item(key = "disabled") { Caption("Your retailer has not enabled customer document uploads.") }
                if (data.items.isEmpty()) item(key = "empty") { Caption("No documents submitted yet.") }
                items(data.items, key = { it.id }, contentType = { "document" }) { item ->
                    Panel {
                        Text(item.label, style = MaterialTheme.typography.titleMedium)
                        Caption(item.status)
                        if (item.feedback.isNotBlank()) Text(item.feedback)
                        if (item.canView) TextButton(onClick = { export = item; saveFile.launch(if (item.contentType == "application/pdf") "document.pdf" else "document.jpg") }, enabled = !state.busy) { Text("Save a copy") }
                        if (data.canUpload && item.review != "superseded" && !item.category.startsWith("guarantor")) DocumentSubmission(data, state.busy, model::enqueue, item)
                        if (item.canDelete) TextButton(onClick = { removing = item }, enabled = !state.busy) { Text("Remove submission") }
                    }
                }
                if (data.next != null) item(key = "more") { OutlinedButton(onClick = model::more, enabled = !state.busy) { Text("Load more") } }
            }
            items(work.filter { it.state != WorkInfo.State.SUCCEEDED }.takeLast(10), key = { "work:${it.id}" }, contentType = { "queued" }) { job ->
                Panel {
                    Text(when (job.state) { WorkInfo.State.RUNNING -> "Uploading securely…"; WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> "Queued · waiting for connection or retry"; WorkInfo.State.CANCELLED -> "Upload cancelled"; else -> "Upload could not finish" })
                    job.outputData.getString("error")?.let { Caption(it) }
                    if (!job.state.isFinished) TextButton(onClick = { model.cancel(job.id) }, enabled = !state.busy) { Text("Cancel upload") }
                }
            }
        }
    }
    removing?.let { item -> CustomerDialog(title = "Remove submission?", message = "This removes access to your pending submission. Files already accepted by staff are managed by your retailer.",
        confirmLabel = "Remove", dismissLabel = "Keep", icon = R.drawable.state_error, destructive = true,
        onConfirm = { model.remove(item); removing = null }, onDismiss = { removing = null }) }
}

@Composable
private fun DocumentSubmission(data: CustomerDocuments, busy: Boolean, enqueue: (Uri, String, String?, String?, () -> Unit) -> Unit, replacement: CustomerDocument? = null) {
    var open by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(replacement?.category ?: "phone_box") }
    var requestId by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Uri?>(null) }
    var consent by remember { mutableStateOf(false) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selected = it }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected = it }
    if (!open) { OutlinedButton(onClick = { open = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (replacement == null) "Add a document" else "Replace this document") }; return }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (replacement == null) "New document" else "Replace ${replacement.label}", style = MaterialTheme.typography.titleMedium)
        if (replacement == null) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, enabled = !busy) { Text(documentLabels[category] ?: "Document") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    documentLabels.filterKeys { !it.startsWith("guarantor") }.forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { category = key; requestId = null; expanded = false }) }
                    data.requests.forEach { request -> DropdownMenuItem(text = { Text("Requested: ${documentLabels[request.category] ?: "Document"}") }, onClick = { category = request.category; requestId = request.id; expanded = false }) }
                }
            }
        }
        data.requests.firstOrNull { it.id == requestId }?.let { Text(it.message) }
        OutlinedButton(onClick = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose photo") }
        OutlinedButton(onClick = { file.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose PDF or image file") }
        if (selected != null) Caption("File selected · up to ${data.maxBytes / (1024 * 1024)} MB")
        Caption(data.notice)
        Row(Modifier.fillMaxWidth()) {
            Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !busy)
            Text("I have permission to share this document with this retailer for this plan.", modifier = Modifier.weight(1f).padding(top = 12.dp))
        }
        Button(onClick = { val uri = selected ?: return@Button; enqueue(uri, category, requestId, replacement?.id) { selected = null; consent = false; open = false } }, enabled = selected != null && consent && !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Preparing securely…" else "Submit for staff review") }
        TextButton(onClick = { selected = null; consent = false; open = false }, enabled = !busy) { Text("Cancel") }
    }
}
