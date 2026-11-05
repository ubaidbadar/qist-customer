package pk.qist.customer.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal data class CustomerSession(val accessToken: String, val expiresAt: String)
internal data class PlanChoice(val id: String, val deviceLabel: String, val retailer: String, val status: String)
internal data class CustomerInstallment(val number: Int, val dueDate: String, val amount: Long, val lateFee: Long, val paidAmount: Long, val status: String) {
    val remaining: Long get() = (amount + lateFee - paidAmount).coerceAtLeast(0)
}
internal data class CustomerPayment(
    val id: String, val receiptNumber: Long, val amount: Long, val method: String, val receivedOn: String,
    val receivedByName: String, val kind: String, val reference: String, val installmentNumbers: List<Int>,
)
internal data class PaymentPage(val items: List<CustomerPayment>, val nextCursor: String?)
internal data class CustomerPlan(
    val id: String, val deviceLabel: String, val retailer: String, val status: String,
    val downPayment: Long, val totalPayable: Long, val paidTotal: Long, val lateFees: Long,
    val count: Int, val frequency: String, val retailerPhone: String, val retailerAddress: String,
    val deviceLockState: String?, val installments: List<CustomerInstallment>,
) {
    val remaining: Long get() = if (status == "cancelled") 0 else (totalPayable + lateFees - paidTotal).coerceAtLeast(0)
    val totalPaid: Long get() = if (status == "cancelled") 0 else downPayment + paidTotal
    val nextInstallment: CustomerInstallment? get() = if (status == "active" || status == "defaulted") installments.firstOrNull { it.remaining > 0 } else null
    val paidCount: Int get() = installments.count { it.status == "paid" }
}
internal data class CustomerOverview(
    val name: String, val phone: String, val contracts: List<PlanChoice>, val plan: CustomerPlan?,
    val payments: PaymentPage, val today: String,
)

/** The API uses integer paisa; never round money through Float/Double. */
internal fun formatAmount(paisa: Long): String = NumberFormat.getNumberInstance(Locale.US).apply {
    maximumFractionDigits = 2
}.format(BigDecimal.valueOf(paisa, 2))

internal fun daysUntil(date: String, today: String): Int? {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
    return try {
        val due = format.parse(date) ?: return null
        val start = format.parse(today) ?: return null
        ((due.time - start.time) / 86_400_000).toInt()
    } catch (_: ParseException) { null }
}
