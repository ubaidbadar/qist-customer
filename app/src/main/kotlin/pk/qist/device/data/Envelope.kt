package pk.qist.device.data

import kotlinx.serialization.Serializable

/** Every successful API response is `{ "data": ... }`. */
@Serializable
data class Envelope<T>(val data: T)
