package pk.qist.customer.data

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class PaymentSample(
    val id: String, val number: Int, val date: String, val amount: String, val method: String,
    val receiptNumber: Long, val receivedBy: String, val kind: String, val reference: String,
    val allocations: List<Int>,
) {
    val label: String get() = if (kind == "down_payment") "Down payment" else
        if (allocations.isEmpty()) "Payment" else "Installment ${allocations.joinToString(", ")}"
}
internal data class Installment(val number: Int, val date: String, val amount: Long, val paidAmount: Long, val lateFee: Long, val status: String)
internal data class PlanChoice(val id: String, val label: String, val retailer: String)
internal data class PaymentAccount(val kind: String, val title: String, val number: String, val bank: String)
internal data class DashboardSample(
    val name: String, val phone: String, val model: String, val amount: String, val dueDate: String,
    val days: Int, val paid: Int, val total: Int, val retailer: String, val advance: Long,
    val installment: Long, val schedule: List<Installment>, val payments: List<PaymentSample>,
    val totalPrice: Long, val paidTotal: Long, val remaining: Long, val frequency: String,
    val status: String, val lockState: String?, val retailerPhone: String, val retailerAddress: String,
    val paymentMethods: List<PaymentAccount>, val contractId: String?, val contracts: List<PlanChoice>,
    val nextCursor: String?,
)
internal data class PaymentPage(val items: List<PaymentSample>, val nextCursor: String?)
internal class CustomerApiException(val status: Int, message: String) : IOException(message)

internal interface CustomerRepository {
    suspend fun requestCode(phone: String)
    suspend fun resetPassword(phone: String, code: String, password: String)
    suspend fun login(phone: String, password: String)
    suspend fun restore(): Boolean
    suspend fun dashboard(contractId: String? = null): DashboardSample
    suspend fun payments(contractId: String, cursor: String): PaymentPage
    suspend fun logout()
    suspend fun clearSession()
}

internal class HttpCustomerRepository(
    private val session: SessionStore,
    baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS).retryOnConnectionFailure(false).build(),
) : CustomerRepository {
    private val base = baseUrl.trimEnd('/').plus('/').toHttpUrl().also {
        require(it.isHttps) { "Customer API must use HTTPS" }
    }
    private var token: String? = null

    override suspend fun restore(): Boolean {
        token = session.read()
        return token != null
    }
    override suspend fun login(phone: String, password: String) {
        val data = request("auth/login", body = buildJsonObject { put("phone", phone); put("password", password) })
        val accessToken = data.requiredText("accessToken")
        session.save(accessToken, data.requiredText("expiresAt"))
        token = accessToken
    }
    override suspend fun requestCode(phone: String) {
        request("auth/forgot-password", body = buildJsonObject { put("phone", phone) })
    }
    override suspend fun resetPassword(phone: String, code: String, password: String) {
        request("auth/reset-password", body = buildJsonObject { put("phone", phone); put("code", code); put("password", password) })
    }
    override suspend fun dashboard(contractId: String?): DashboardSample = withContext(Dispatchers.IO) {
        parseOverview(request("overview", query = mapOf("contractId" to contractId), authorized = true))
    }
    override suspend fun payments(contractId: String, cursor: String): PaymentPage = withContext(Dispatchers.IO) {
        parsePayments(request("payments", query = mapOf("contractId" to contractId, "cursor" to cursor), authorized = true))
    }
    override suspend fun logout() {
        request("auth/logout", body = buildJsonObject {}, authorized = true)
        clearSession()
    }
    override suspend fun clearSession() {
        token = null
        session.clear()
    }
    private suspend fun request(path: String, body: JsonObject? = null, query: Map<String, String?> = emptyMap(), authorized: Boolean = false): JsonObject = withContext(Dispatchers.IO) {
        val url = base.newBuilder().addPathSegments(path).apply {
            query.forEach { (key, value) -> if (value != null) addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder().url(url).header("Accept", "application/json").apply {
            if (authorized) header("Authorization", "Bearer ${token ?: throw CustomerApiException(401, "Please sign in again.")}")
            if (body != null) post(body.toString().toRequestBody("application/json".toMediaType()))
        }.build()
        val response = suspendCancellableCoroutine<okhttp3.Response> { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    continuation.resume(response) { _, value, _ -> value.close() }
                }
            })
        }
        response.use {
            val content = response.body.string()
            val envelope = try { Json.parseToJsonElement(content).jsonObject } catch (_: IllegalArgumentException) {
                if (!response.isSuccessful) throw CustomerApiException(response.code, "Request failed (${response.code}). Please try again.")
                throw IOException("The server returned an invalid response. Please try again.")
            }
            if (!response.isSuccessful) {
                val message = (envelope["error"] as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
                throw CustomerApiException(response.code, message ?: "Request failed (${response.code}). Please try again.")
            }
            envelope["data"] as? JsonObject ?: throw IOException("The server response is missing data.")
        }
    }
}

internal fun formatAmount(paisa: Long): String = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
    .format(BigDecimal.valueOf(paisa, 2))
internal fun displayDate(value: String): String {
    if (value.isBlank()) return "—"
    return SimpleDateFormat("dd MMM yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(calendarDate(value))
}
private fun calendarDate(value: String) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    isLenient = false; timeZone = TimeZone.getTimeZone("UTC")
}.parse(value.take(10)) ?: throw IOException("Invalid date in server response.")
private fun JsonObject.requiredText(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull ?: throw IOException("Missing $key in server response.")
private fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull.orEmpty()
private fun JsonObject.amount(key: String) = (get(key) as? JsonPrimitive)?.longOrNull ?: throw IOException("Invalid $key in server response.")
private fun JsonObject.rows(key: String) = (get(key) as? JsonArray)?.map { it.jsonObject } ?: throw IOException("Missing $key in server response.")
internal fun parsePayments(data: JsonObject): PaymentPage = PaymentPage(data.rows("items").map { row ->
    val allocations = row.rows("allocations").map { it.getValue("number").jsonPrimitive.int }
    PaymentSample(row.requiredText("id"), allocations.firstOrNull() ?: 0, displayDate(row.requiredText("receivedOn")),
        formatAmount(row.amount("amount")), row.requiredText("method"), row.amount("receiptNumber"),
        row.text("receivedByName"), row.requiredText("kind"), row.text("reference"), allocations)
}, data["nextCursor"]?.jsonPrimitive?.contentOrNull)
internal fun parseOverview(data: JsonObject): DashboardSample {
    val profile = data.getValue("profile").jsonObject
    val contract = data["contract"] as? JsonObject
    val schedule = contract?.rows("installments")?.map { row ->
        Installment(row.getValue("number").jsonPrimitive.int, row.requiredText("dueDate"), row.amount("amount"), row.amount("paidAmount"), row.amount("lateFee"), row.requiredText("status"))
    }.orEmpty()
    val next = schedule.firstOrNull { it.status != "paid" && it.status != "waived" && it.amount + it.lateFee > it.paidAmount }
    val page = parsePayments(data.getValue("payments").jsonObject)
    val advance = contract?.amount("downPayment") ?: 0L
    val payable = contract?.amount("totalPayable") ?: 0L
    val paid = contract?.amount("paidTotal") ?: 0L
    return DashboardSample(
        name = profile.requiredText("name"), phone = profile.requiredText("phone"),
        model = contract?.text("deviceLabel").orEmpty(),
        amount = formatAmount(next?.let { (it.amount + it.lateFee - it.paidAmount).coerceAtLeast(0) } ?: 0L),
        dueDate = next?.let { displayDate(it.date) } ?: "—",
        days = next?.let { ((calendarDate(it.date).time - calendarDate(data.requiredText("today")).time) / 86_400_000L).toInt() } ?: 0,
        paid = schedule.count { it.status == "paid" }, total = schedule.size,
        retailer = contract?.text("retailer").orEmpty(), advance = advance, installment = next?.amount ?: 0L,
        schedule = schedule, payments = page.items, totalPrice = advance + payable,
        paidTotal = advance + paid, remaining = (payable + (contract?.amount("lateFees") ?: 0L) - paid).coerceAtLeast(0L),
        frequency = contract?.text("frequency").orEmpty(), status = contract?.text("status").orEmpty(),
        lockState = contract?.get("deviceLockState")?.jsonPrimitive?.contentOrNull,
        retailerPhone = contract?.text("retailerPhone").orEmpty(), retailerAddress = contract?.text("retailerAddress").orEmpty(),
        paymentMethods = contract?.get("paymentMethods")?.jsonArray?.map { value ->
            val row = value.jsonObject
            PaymentAccount(row.text("kind"), row.text("accountTitle"), row.text("accountNumber"), row.text("bankName"))
        }.orEmpty(),
        contractId = contract?.text("id"), contracts = data.rows("contracts").map { PlanChoice(it.requiredText("id"), it.text("deviceLabel"), it.text("retailer")) },
        nextCursor = page.nextCursor,
    )
}
