package pk.qist.customer.data

internal interface CustomerRepository {
    fun verify(code: String): Boolean
    fun dashboard(): DashboardSample
    fun phones(): List<DashboardSample>
}

internal data class PaymentSample(val number: Int, val date: String, val amount: String, val method: String = "Cash")

internal data class PaymentMethodSample(val title: String, val accountTitle: String = "", val accountNumber: String = "", val bank: String = "", val cash: Boolean = false)

internal data class DashboardSample(
    val name: String,
    val id: String = "demo-primary",
    val completed: Boolean = false,
    val paidOffDate: String = "",
    val retailerAddress: String = "[SHOP ADDRESS]",
    val paymentMethods: List<PaymentMethodSample> = emptyList(),
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
        paymentMethods = listOf(
            PaymentMethodSample("Cash at shop", cash = true),
            PaymentMethodSample("Easypaisa", "[ACCOUNT TITLE]", "[03XX XXXXXXX]"),
            PaymentMethodSample("JazzCash", "[ACCOUNT TITLE]", "[03XX XXXXXXX]"),
            PaymentMethodSample("Bank transfer", "[ACCOUNT TITLE]", "[IBAN]", "[BANK NAME]"),
        ),
        payments = listOf(
            PaymentSample(3, "12 Oct 2026", "7,500"),
            PaymentSample(2, "12 Sep 2026", "7,500"),
            PaymentSample(1, "12 Aug 2026", "7,500"),
        ),
    )

    private val plans = listOf(
        sample,
        sample.copy(id = "demo-second", model = "Demo Phone 2", dueDate = "09 Nov 2026", days = 2, paid = 2, payments = sample.payments.drop(1)),
        completedPhone("demo-completed-1", "Demo Phone 3", listOf("12 May 2026", "12 Jun 2026", "12 Jul 2026", "12 Aug 2026", "12 Sep 2026", "12 Oct 2026")),
        completedPhone("demo-completed-2", "Demo Phone 4", listOf("12 Apr 2026", "12 May 2026", "12 Jun 2026", "12 Jul 2026", "12 Aug 2026", "12 Sep 2026")),
    )
    private fun completedPhone(id: String, model: String, dates: List<String>) = sample.copy(
        id = id, model = model, completed = true, paidOffDate = dates.last(), paid = dates.size,
        amount = "0", dueDate = "—", schedule = dates,
        payments = dates.mapIndexed { index, date -> PaymentSample(index + 1, date, "7,500") }.reversed(),
    )
    override fun dashboard() = sample
    override fun phones() = plans
}
