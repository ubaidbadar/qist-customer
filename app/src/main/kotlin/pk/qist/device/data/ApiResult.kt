package pk.qist.device.data

/** Every way a call to the device API can end. Callers branch on this with an exhaustive `when`. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    /** 401: the credential or enrolment token is not (or no longer) valid. */
    data object Unauthorized : ApiResult<Nothing>

    /** No connection, a timeout, or a dropped connection. Worth retrying later. */
    data object NetworkError : ApiResult<Nothing>

    /** The server answered with an error, or with a body this app could not read. */
    data class ServerError(val status: Int) : ApiResult<Nothing>
}
