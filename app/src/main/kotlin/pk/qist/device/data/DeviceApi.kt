package pk.qist.device.data

/** The three calls the phone makes. An interface so the check-in and enrolment logic can be tested with a fake. */
interface DeviceApi {
    suspend fun enroll(request: EnrollRequest): ApiResult<DeviceCredential>

    suspend fun checkIn(credential: String, request: CheckInRequest): ApiResult<CheckInResponse>

    suspend fun acknowledge(credential: String, commandId: String, request: AckRequest): ApiResult<Unit>
}
