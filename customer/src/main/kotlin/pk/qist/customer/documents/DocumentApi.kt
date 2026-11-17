package pk.qist.customer.documents

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import pk.qist.customer.data.CustomerApiException
import pk.qist.customer.data.CustomerResponseException
import pk.qist.customer.data.SessionStore

internal val documentLabels = linkedMapOf(
    "customer_cnic_front" to "My CNIC · front", "customer_cnic_back" to "My CNIC · back",
    "phone_box" to "Phone box / IMEI", "warranty" to "Warranty card", "invoice" to "Invoice",
    "agreement" to "Signed agreement", "guarantee" to "Signed guarantee",
    "guarantor_cnic_front" to "Guarantor CNIC · front", "guarantor_cnic_back" to "Guarantor CNIC · back",
)
internal data class CustomerDocument(
    val id: String, val category: String, val processing: String, val review: String,
    val feedback: String, val version: Int, val canView: Boolean, val canDelete: Boolean, val contentType: String,
) {
    val label: String get() = documentLabels[category] ?: "Document"
    val status: String get() = when (processing) {
        "uploading" -> "Waiting for upload"; "processing" -> "Checking file"; "failed" -> "File checks delayed"
        "rejected" -> "File not accepted"; "deleted" -> "Removed"
        else -> when (review) { "accepted" -> "Accepted by staff"; "rejected" -> "Needs replacement"; "superseded" -> "Replaced"; else -> "Pending staff review" }
    }
}
internal data class CustomerDocumentRequest(val id: String, val category: String, val message: String)
internal data class CustomerDocuments(
    val items: List<CustomerDocument>, val requests: List<CustomerDocumentRequest>, val next: String?,
    val canUpload: Boolean, val maxBytes: Long, val notice: String,
)
internal fun parseDocument(row: JsonObject) = CustomerDocument(
    row.string("_id"), row.string("category"), row.string("processing"), row.string("review"), row.string("feedback"),
    row["version"]?.jsonPrimitive?.intOrNull ?: throw CustomerResponseException("Invalid document version"),
    row["canView"]?.jsonPrimitive?.booleanOrNull == true, row["canDelete"]?.jsonPrimitive?.booleanOrNull == true, row.string("contentType"),
)
internal fun parseDocuments(data: JsonObject): CustomerDocuments {
    val policy = data["policy"]?.jsonObject ?: throw CustomerResponseException("Document settings are missing")
    return CustomerDocuments(
        data["items"]?.jsonArray?.map { parseDocument(it.jsonObject) } ?: throw CustomerResponseException("Document list is missing"),
        data["requests"]?.jsonArray?.map { val row = it.jsonObject; CustomerDocumentRequest(row.string("_id"), row.string("category"), row.string("message")) }.orEmpty(),
        data["nextCursor"]?.jsonPrimitive?.contentOrNull,
        data["canUpload"]?.jsonPrimitive?.booleanOrNull == true,
        policy["maxFileBytes"]?.jsonPrimitive?.longOrNull?.coerceIn(1024, 10 * 1024 * 1024) ?: 10 * 1024 * 1024,
        policy.string("retentionNotice"),
    )
}
private fun JsonObject.string(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()

/** Uses the same encrypted session store as sign-in; bearer tokens never enter WorkManager data. */
internal class DocumentApi(private val sessions: SessionStore, baseUrl: String, private val expectedScope: String? = null,
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build(),
) {
    private val base = baseUrl.trimEnd('/').plus('/').toHttpUrl().also { require(it.isHttps) }
    suspend fun list(contractId: String, cursor: String? = null): CustomerDocuments {
        val url = base.newBuilder().addPathSegment("documents").addQueryParameter("contractId", contractId)
            .apply { if (cursor != null) addQueryParameter("cursor", cursor) }.build()
        return parseDocuments(json(Request.Builder().url(url).get()))
    }
    suspend fun create(contractId: String, category: String, key: String, requestId: String?, replacesId: String?): CustomerDocument = parseDocument(
        json(Request.Builder().url(base.resolve("documents")!!).post(buildJsonObject {
            put("contractId", contractId); put("category", category); put("idempotencyKey", key); put("consent", true)
            if (requestId != null) put("requestId", requestId)
            if (replacesId != null) put("replacesId", replacesId)
        }.toString().toRequestBody("application/json".toMediaType()))))
    suspend fun upload(id: String, body: RequestBody): CustomerDocument = parseDocument(json(Request.Builder().url(base.resolve("documents/$id/content")!!).put(body)))
    suspend fun remove(item: CustomerDocument): CustomerDocument = parseDocument(json(Request.Builder()
        .url(base.resolve("documents/${item.id}?version=${item.version}")!!).delete()))
    suspend fun export(item: CustomerDocument, write: (ResponseBody) -> Unit) {
        execute(Request.Builder().url(base.resolve("documents/${item.id}/content")!!).get()).use { response ->
            checkStatus(response)
            if (response.body.contentLength() > 10 * 1024 * 1024) throw IOException("Document is too large")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { write(response.body) }
        }
    }
    private suspend fun json(builder: Request.Builder): JsonObject = execute(builder).use { response ->
        checkStatus(response)
        if (response.body.contentLength() > 256 * 1024) throw IOException("Response is too large")
        val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val source = response.body.source()
            source.request(256 * 1024 + 1L)
            if (source.buffer.size > 256 * 1024) throw IOException("Response is too large")
            source.readUtf8()
        }
        try { Json.parseToJsonElement(text).jsonObject["data"]?.jsonObject ?: throw CustomerResponseException("Document response is missing") }
        catch (_: IllegalArgumentException) { throw CustomerResponseException("Invalid document response") }
    }
    private fun checkStatus(response: Response) {
        if (!response.isSuccessful) throw CustomerApiException(response.code, when (response.code) {
            401 -> "Your session has expired. Sign in again."
            403, 404 -> "This document is unavailable for your account."
            409 -> "This document changed. Refresh and try again."
            413 -> "Choose a smaller file."
            429, 503 -> "Document uploads are busy. Please try again shortly."
            else -> "Could not complete the document request. Please try again."
        })
    }
    private suspend fun execute(builder: Request.Builder): Response {
        val token = sessions.read() ?: throw CustomerApiException(401, "Please sign in again.")
        if (expectedScope != null && sessionScope(token) != expectedScope) throw CustomerApiException(401, "The account changed. Choose the file again.")
        val call = client.newCall(builder.header("Authorization", "Bearer $token").build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (!continuation.isCancelled) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) { continuation.resume(response) { _, value, _ -> value.close() } }
            })
        }
    }
}
