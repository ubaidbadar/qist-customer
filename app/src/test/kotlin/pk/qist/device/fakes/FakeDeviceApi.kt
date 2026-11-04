package pk.qist.device.fakes

import pk.qist.device.data.AckRequest
import pk.qist.device.data.ApiResult
import pk.qist.device.data.CheckInRequest
import pk.qist.device.data.CheckInResponse
import pk.qist.device.data.DeviceApi
import pk.qist.device.data.DeviceCredential
import pk.qist.device.data.EnrollRequest

/** Answers with whatever the test set, and records what the app sent. */
class FakeDeviceApi : DeviceApi {
    var enrollResult: ApiResult<DeviceCredential> = ApiResult.NetworkError
    var checkInResult: ApiResult<CheckInResponse> = ApiResult.NetworkError
    var ackResult: ApiResult<Unit> = ApiResult.Success(Unit)

    val enrollRequests = mutableListOf<EnrollRequest>()
    val checkIns = mutableListOf<Pair<String, CheckInRequest>>()
    val acks = mutableListOf<Pair<String, AckRequest>>()

    override suspend fun enroll(request: EnrollRequest) = enrollResult.also { enrollRequests += request }

    override suspend fun checkIn(credential: String, request: CheckInRequest) =
        checkInResult.also { checkIns += credential to request }

    override suspend fun acknowledge(credential: String, commandId: String, request: AckRequest) =
        ackResult.also { acks += commandId to request }
}
