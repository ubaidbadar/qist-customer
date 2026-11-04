package pk.qist.device.data

import androidx.datastore.core.CorruptionException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DeviceStateSerializerTest {
    /** Reverses the bytes: enough to prove the serializer only ever writes what the sealer produced. */
    private val reversing = object : Sealer {
        override fun seal(plain: ByteArray) = plain.reversedArray()

        override fun open(sealed: ByteArray) = sealed.reversedArray()
    }
    private val serializer = DeviceStateSerializer(reversing, Json)

    @Test
    fun `state survives a write and read`() = runTest {
        val state = DeviceState(
            enrolment = Enrolment.Enrolled("d1", "d1.secret"),
            lockState = LockState.LOCKED,
            lockScreen = LockScreen(locked = true, amountDue = 1_500_00, branchPhone = "0300"),
            checkInSeconds = 1800
        )
        val bytes = ByteArrayOutputStream().also { serializer.writeTo(state, it) }.toByteArray()

        assertEquals(state, serializer.readFrom(ByteArrayInputStream(bytes)))
    }

    @Test
    fun `the file on disk never holds the credential in plain text`() = runTest {
        val bytes = ByteArrayOutputStream()
            .also { serializer.writeTo(DeviceState(enrolment = Enrolment.Enrolled("d1", "d1.secret")), it) }
            .toByteArray()

        assertFalse(bytes.decodeToString().contains("d1.secret"))
    }

    @Test
    fun `an empty file reads as a fresh install`() = runTest {
        assertEquals(DeviceState(), serializer.readFrom(ByteArrayInputStream(ByteArray(0))))
    }

    @Test(expected = CorruptionException::class)
    fun `a garbled file is reported as corruption`() = runTest {
        serializer.readFrom(ByteArrayInputStream("not json".toByteArray()))
    }

    @Test
    fun `secrets never appear in toString`() {
        assertFalse(Enrolment.Enrolled("d1", "d1.secret").toString().contains("secret"))
        assertFalse(Enrolment.Pending("qr-token").toString().contains("qr-token"))
        assertFalse(DeviceCredential("d1", "d1.secret", 900).toString().contains("secret"))
    }
}
