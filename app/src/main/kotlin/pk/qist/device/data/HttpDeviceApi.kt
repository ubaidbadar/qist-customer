package pk.qist.device.data

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * The device API over OkHttp. Each call returns an [ApiResult]; network failures and unreadable bodies are turned
 * into results here, at the edge, so nothing above this class deals with exceptions. Nothing is logged.
 */
class HttpDeviceApi(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl,
    private val json: Json,
    private val io: CoroutineDispatcher
) : DeviceApi {
    override suspend fun enroll(request: EnrollRequest) =
        post("device/enroll", credential = null, json.encodeToString(request), DeviceCredential.serializer())

    override suspend fun checkIn(credential: String, request: CheckInRequest) =
        post("device/checkin", credential, json.encodeToString(request), CheckInResponse.serializer())

    override suspend fun acknowledge(credential: String, commandId: String, request: AckRequest): ApiResult<Unit> =
        when (
            val result =
                post(
                    "device/commands/$commandId/ack",
                    credential,
                    json.encodeToString(request),
                    JsonElement.serializer()
                )
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.NetworkError -> ApiResult.NetworkError
            is ApiResult.ServerError -> result
        }

    private suspend fun <T> post(path: String, credential: String?, body: String, data: KSerializer<T>): ApiResult<T> =
        withContext(io) {
            val request = Request.Builder()
                .url(baseUrl.newBuilder().addPathSegments(path).build())
                .post(body.toRequestBody(JSON))
                .apply { if (credential != null) header("Authorization", "Bearer $credential") }
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == UNAUTHORIZED -> ApiResult.Unauthorized
                        !response.isSuccessful -> ApiResult.ServerError(response.code)
                        else -> decode(response.body.string(), data, response.code)
                    }
                }
            } catch (_: IOException) {
                ApiResult.NetworkError
            }
        }

    private fun <T> decode(body: String, data: KSerializer<T>, status: Int): ApiResult<T> = try {
        ApiResult.Success(json.decodeFromString(Envelope.serializer(data), body).data)
    } catch (_: SerializationException) {
        ApiResult.ServerError(status)
    } catch (_: IllegalArgumentException) {
        ApiResult.ServerError(status)
    }

    private companion object {
        const val UNAUTHORIZED = 401
        val JSON = "application/json".toMediaType()
    }
}
