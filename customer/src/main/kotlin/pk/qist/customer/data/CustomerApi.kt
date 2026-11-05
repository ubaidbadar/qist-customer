package pk.qist.customer.data

import java.io.IOException

internal class CustomerApiException(val status: Int, message: String) : IOException(message)

internal interface CustomerApi {
    suspend fun login(phone: String, password: String): CustomerSession
    suspend fun requestPassword(phone: String)
    suspend fun resetPassword(phone: String, code: String, password: String)
    suspend fun overview(token: String, contractId: String? = null): CustomerOverview
    suspend fun payments(token: String, contractId: String, cursor: String): PaymentPage
    suspend fun logout(token: String)
}

internal interface CustomerSessionStore {
    suspend fun read(): String?
    suspend fun write(token: String?)
}
