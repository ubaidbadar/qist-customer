package pk.qist.device.lock

import androidx.compose.runtime.Immutable

/** The lock screen's text, ready to show. Null means "leave that part out". */
@Immutable
data class LockScreenUi(
    val message: String,
    val amountDue: String?,
    val nextDueDate: String?,
    val branchName: String?,
    val branchPhone: String?,
    val overdueDays: Int? = null
)
