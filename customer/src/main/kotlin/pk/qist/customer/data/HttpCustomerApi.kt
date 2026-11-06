package pk.qist.customer.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import pk.qist.customer.BuildConfig

internal class HttpCustomerApi(
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(false).build(),
    baseUrl: String = BuildConfig.CUSTOMER_API_URL,
) : CustomerApi {
    private val base = baseUrl.toHttpUrl()
    override suspend fun login(phone: String, password: String): CustomerSession {
        val data = request("auth/login", body = JSONObject().put("phone", phone).put("password", password))
        return CustomerSession(data.getString("accessToken"), data.getString("expiresAt"))
    }
    override suspend fun requestPassword(phone: String) { request("auth/forgot-password", body = JSONObject().put("phone", phone)) }
    override suspend fun resetPassword(phone: String, code: String, password: String) {
        request("auth/reset-password", body = JSONObject().put("phone", phone).put("code", code).put("password", password))
    }
    override suspend fun overview(token: String, contractId: String?): CustomerOverview = withContext(Dispatchers.Default) {
        try { parseOverview(request("overview", token, query = contractId?.let { mapOf("contractId" to it) }.orEmpty())) }
        catch (_: JSONException) { throw IOException("The server returned incomplete account data. Please try again.") }
    }
    override suspend fun payments(token: String, contractId: String, cursor: String): PaymentPage = withContext(Dispatchers.Default) {
        try { parsePayments(request("payments", token, query = mapOf("contractId" to contractId, "cursor" to cursor))) }
        catch (_: JSONException) { throw IOException("The server returned incomplete payment data. Please try again.") }
    }
    override suspend fun logout(token: String) { request("auth/logout", token, body = JSONObject()) }

    private suspend fun request(path: String, token: String? = null, body: JSONObject? = null, query: Map<String, String> = emptyMap()): JSONObject =
        suspendCancellableCoroutine { continuation ->
            val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (key, value) -> addQueryParameter(key, value) } }.build()
            val request = Request.Builder().url(url).header("Accept", "application/json").apply {
                if (token != null) header("Authorization", "Bearer $token")
                if (body != null) post(body.toString().toRequestBody("application/json".toMediaType()))
            }.build()
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(IOException("Could not connect. Check your internet and try again.", e))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            val json = try { JSONObject(it.body.string()) } catch (_: JSONException) { throw IOException("The server returned an unreadable response. Please try again.") }
                            if (!it.isSuccessful) throw CustomerApiException(it.code, json.optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank) ?: "Could not complete the request. Please try again.")
                            json.getJSONObject("data")
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                    catch (_: JSONException) { if (continuation.isActive) continuation.resumeWithException(IOException("The server returned incomplete data. Please try again.")) }
                }
            })
        }
}

private fun <T> JSONArray.rows(read: (JSONObject) -> T): List<T> = List(length()) { index -> read(getJSONObject(index)) }
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)

internal fun parsePayments(data: JSONObject): PaymentPage = PaymentPage(data.getJSONArray("items").rows { payment ->
    CustomerPayment(
        payment.getString("id"), payment.getLong("receiptNumber"), payment.getLong("amount"), payment.getString("method"), payment.getString("receivedOn"),
        payment.getString("receivedByName"), payment.getString("kind"), payment.getString("reference"),
        payment.getJSONArray("allocations").rows { it.getInt("number") },
    )
}, data.nullableString("nextCursor"))

internal fun parseOverview(data: JSONObject): CustomerOverview {
    val profile = data.getJSONObject("profile")
    val plan = if (data.isNull("contract")) null else data.getJSONObject("contract").let { contract ->
        CustomerPlan(
            contract.getString("id"), contract.getString("deviceLabel"), contract.getString("retailer"), contract.getString("status"),
            contract.getLong("downPayment"), contract.getLong("totalPayable"), contract.getLong("paidTotal"), contract.getLong("lateFees"),
            contract.getInt("count"), contract.getString("frequency"), contract.getString("retailerPhone"), contract.getString("retailerAddress"),
            contract.nullableString("deviceLockState"), contract.getJSONArray("installments").rows { row ->
                CustomerInstallment(row.getInt("number"), row.getString("dueDate"), row.getLong("amount"), row.getLong("lateFee"), row.getLong("paidAmount"), row.getString("status"))
            },
            contract.optJSONArray("paymentMethods")?.rows { method ->
                CustomerPaymentMethod(method.getString("id"), method.getString("kind"), method.optString("accountTitle"), method.optString("accountNumber"), method.optString("bankName"))
            }.orEmpty(),
        )
    }
    return CustomerOverview(profile.getString("name"), profile.getString("phone"), data.getJSONArray("contracts").rows {
        PlanChoice(it.getString("id"), it.getString("deviceLabel"), it.getString("retailer"), it.getString("status"))
    }, plan, parsePayments(data.getJSONObject("payments")), data.getString("today"))
}
