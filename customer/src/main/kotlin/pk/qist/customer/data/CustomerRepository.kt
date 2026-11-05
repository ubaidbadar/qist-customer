package pk.qist.customer.data

internal interface CustomerRepository {
    fun verify(code: String): Boolean
    fun dashboard(): DashboardSample
}

internal data class PaymentSample(val number: Int, val date: String, val amount: String, val method: String = "Cash")

internal data class DashboardSample(
    val name: String,
    val model: String,
    val amount: String,
    val dueDate: String,
    val days: Int,
    val paid: Int,
    val total: Int,
    val retailer: String = "Demo Retailer",
    val advance: Int = 15000,
    val installment: Int = 7500,
    val schedule: List<String> = listOf("12 Aug 2026", "12 Sep 2026", "12 Oct 2026", "12 Nov 2026", "12 Dec 2026", "12 Jan 2027"),
    val payments: List<PaymentSample>,
)

internal object DemoCustomerRepository : CustomerRepository {
    override fun verify(code: String) = code == "123456"
    private val sample = DashboardSample(
        name = "Demo Customer",
        model = "Demo Phone",
        amount = "7,500",
        dueDate = "12 Nov 2026",
        days = 8,
        paid = 3,
        total = 6,
        payments = listOf(
            PaymentSample(3, "12 Oct 2026", "7,500"),
            PaymentSample(2, "12 Sep 2026", "7,500"),
            PaymentSample(1, "12 Aug 2026", "7,500"),
        ),
    )

    override fun dashboard() = sample
}
