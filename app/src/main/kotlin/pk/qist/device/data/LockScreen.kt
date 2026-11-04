package pk.qist.device.data

import kotlinx.serialization.Serializable

/** What the customer sees on a locked phone, exactly as the server sends it. Money is paisa, dates YYYY-MM-DD. */
@Serializable
data class LockScreen(
    val locked: Boolean = false,
    val customerName: String = "",
    val deviceLabel: String = "",
    val amountDue: Long = 0,
    val nextDueDate: String? = null,
    val branchName: String = "",
    val branchPhone: String = "",
    val message: String = ""
)
