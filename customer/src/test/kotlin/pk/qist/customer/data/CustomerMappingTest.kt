package pk.qist.customer.data

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

internal fun overviewFixture(): JsonObject = Json.parseToJsonElement(
    requireNotNull(CustomerMappingTest::class.java.getResource("/overview.json")).readText(),
).jsonObject

class CustomerMappingTest {
    @Test fun `money preserves paisa and large amounts`() {
        assertEquals("40,000.5", formatAmount(4000050))
        assertEquals("0.01", formatAmount(1))
        assertEquals("30,000,000", formatAmount(3000000000L))
    }
    @Test fun `overview uses paid amounts rather than paid row position`() {
        val data = parseOverview(overviewFixture())
        assertEquals(1, data.paid)
        assertEquals("7,550", data.amount)
        assertEquals(-7, data.days)
        assertEquals(2250000L, data.paidTotal)
        assertEquals(2755050L, data.remaining)
        assertEquals(5000050L, data.totalPrice)
        assertEquals("weekly", data.frequency)
        assertNull(data.lockState)
        assertEquals(2000050L, data.schedule.last().amount)
        assertEquals("jazzcash", data.paymentMethods.single().kind)
        assertEquals(2, data.contracts.size)
    }
    @Test fun `receipts retain identity allocation method reference and staff`() {
        val data = parseOverview(overviewFixture())
        val payment = data.payments.first()
        assertEquals("payment-a", payment.id)
        assertEquals(42L, payment.receiptNumber)
        assertEquals(listOf(1, 2), payment.allocations)
        assertEquals("jazzcash", payment.method)
        assertEquals("Staff", payment.receivedBy)
        assertEquals("REF-1", payment.reference)
        assertEquals("Down payment", data.payments.last().label)
        assertEquals("payment-b", data.nextCursor)
    }
    @Test fun `empty account has no fabricated plan or payments`() {
        val data = parseOverview(Json.parseToJsonElement("""{"profile":{"name":"Customer","phone":"03001234567"},"today":"2026-11-07","contract":null,"contracts":[],"payments":{"items":[],"nextCursor":null}}""").jsonObject)
        assertNull(data.contractId)
        assertTrue(data.schedule.isEmpty())
        assertTrue(data.payments.isEmpty())
        assertEquals(0, data.total)
        assertEquals(0L, data.remaining)
    }
    @Test fun `completed plan has no next installment`() {
        val fixture = overviewFixture()
        val contract = fixture.getValue("contract").jsonObject.toMutableMap()
        contract["installments"] = JsonArray(contract.getValue("installments").jsonArray.map { row ->
            JsonObject(row.jsonObject + ("status" to JsonPrimitive("paid")))
        })
        val data = parseOverview(JsonObject(fixture + ("contract" to JsonObject(contract))))
        assertEquals("0", data.amount)
        assertEquals("—", data.dueDate)
        assertEquals(3, data.paid)
    }
    @Test fun `overview carries the real inbox without a separate request`() {
        val fixture = overviewFixture()
        val updated = kotlinx.serialization.json.JsonObject(fixture + ("notifications" to kotlinx.serialization.json.Json.parseToJsonElement("""[{"id":"notice-a","contractId":"plan-a","kind":"payment","title":"Payment recorded","body":"Open Payments","createdAt":"2026-11-07T00:00:00.000Z","read":false}]""")))
        val data = parseOverview(updated)
        assertEquals(1, data.notifications.size); assertFalse(data.notifications.single().read)
        assertEquals("plan-a", data.notifications.single().contractId)
    }

}
