package pk.qist.device.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Reads and writes [DeviceState] as JSON, sealed by [sealer] so the file is unreadable outside the app. */
class DeviceStateSerializer(private val sealer: Sealer, private val json: Json) : Serializer<DeviceState> {
    override val defaultValue = DeviceState()

    override suspend fun readFrom(input: InputStream): DeviceState {
        val sealed = input.readBytes()
        if (sealed.isEmpty()) return defaultValue
        return try {
            json.decodeFromString(DeviceState.serializer(), sealer.open(sealed).decodeToString())
        } catch (e: GeneralSecurityException) {
            throw CorruptionException("State file could not be decrypted", e)
        } catch (e: SerializationException) {
            throw CorruptionException("State file could not be read", e)
        }
    }

    override suspend fun writeTo(t: DeviceState, output: OutputStream) {
        output.write(sealer.seal(json.encodeToString(DeviceState.serializer(), t).encodeToByteArray()))
    }
}
