package pk.qist.device.enrolment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pk.qist.device.AppInfo
import pk.qist.device.data.ApiResult
import pk.qist.device.data.DeviceCredential
import pk.qist.device.data.DeviceState
import pk.qist.device.data.Enrolment
import pk.qist.device.fakes.FakeDeviceApi
import pk.qist.device.fakes.FakeDevicePolicy
import pk.qist.device.fakes.InMemoryStore
import pk.qist.device.fakes.RecordingScheduler

class EnrollerTest {
    private val api = FakeDeviceApi()
    private val policy = FakeDevicePolicy()
    private val scheduler = RecordingScheduler()

    private fun enroller(store: InMemoryStore) = Enroller(api, store, policy, scheduler, AppInfo("1.0", "14"))

    @Test
    fun `accepting a QR token stores it and queues enrolment`() = runTest {
        val store = InMemoryStore()

        enroller(store).accept("qr-token")

        assertEquals(Enrolment.Pending("qr-token"), store.value.enrolment)
        assertEquals(listOf("enrol"), scheduler.requests)
    }

    @Test
    fun `a token arriving again never replaces an existing credential`() = runTest {
        val store = InMemoryStore(DeviceState(enrolment = Enrolment.Enrolled("d1", "d1.secret")))

        enroller(store).accept("qr-token")

        assertEquals(Enrolment.Enrolled("d1", "d1.secret"), store.value.enrolment)
        assertEquals(emptyList<String>(), scheduler.requests)
    }

    @Test
    fun `a successful enrolment stores the credential, protects the phone and starts check-ins`() = runTest {
        val store = InMemoryStore(DeviceState(enrolment = Enrolment.Pending("qr-token")))
        api.enrollResult = ApiResult.Success(DeviceCredential("d1", "d1.secret", 1800))

        val outcome = enroller(store).enrol()

        assertEquals(EnrolOutcome.Enrolled(1800), outcome)
        assertEquals(Enrolment.Enrolled("d1", "d1.secret"), store.value.enrolment)
        assertEquals(1800L, store.value.checkInSeconds)
        assertEquals(listOf("protect"), policy.calls)
        assertEquals(listOf("every 1800 s", "check in now"), scheduler.requests)
        assertEquals("qr-token", api.enrollRequests.single().token)
        assertEquals("1.0", api.enrollRequests.single().appVersion)
    }

    @Test
    fun `a refused token is remembered and the phone is left unprotected`() = runTest {
        val store = InMemoryStore(DeviceState(enrolment = Enrolment.Pending("old-token")))
        api.enrollResult = ApiResult.Unauthorized

        assertEquals(EnrolOutcome.Rejected, enroller(store).enrol())
        assertEquals(Enrolment.Rejected, store.value.enrolment)
        assertEquals(emptyList<String>(), policy.calls)
    }

    @Test
    fun `network and server errors keep the token for a retry`() = runTest {
        val store = InMemoryStore(DeviceState(enrolment = Enrolment.Pending("qr-token")))

        api.enrollResult = ApiResult.NetworkError
        assertEquals(EnrolOutcome.RetryLater, enroller(store).enrol())
        api.enrollResult = ApiResult.ServerError(503)
        assertEquals(EnrolOutcome.RetryLater, enroller(store).enrol())

        assertEquals(Enrolment.Pending("qr-token"), store.value.enrolment)
    }

    @Test
    fun `without a waiting token there is nothing to do and no call is made`() = runTest {
        assertEquals(EnrolOutcome.NothingToDo, enroller(InMemoryStore()).enrol())
        assertEquals(0, api.enrollRequests.size)
    }
}
