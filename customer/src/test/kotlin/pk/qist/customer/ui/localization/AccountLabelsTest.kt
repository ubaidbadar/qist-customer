package pk.qist.customer.ui.localization

import org.junit.Assert.*
import org.junit.Test
import pk.qist.customer.data.PaymentSample

class AccountLabelsTest {
    private val payment = PaymentSample("id", 1, "14 Nov 2026", "100", "cash", 1, "Shop", "installment", "", listOf(1, 2))

    @Test fun `payment descriptions translate down payments allocations and unallocated receipts`() {
        assertEquals("Installment 1, 2", paymentLabel(payment, false))
        assertEquals("قسط 1، 2", paymentLabel(payment, true))
        assertEquals("پیشگی ادائیگی", paymentLabel(payment.copy(kind = "down_payment"), true))
        assertEquals("ادائیگی", paymentLabel(payment.copy(allocations = emptyList()), true))
    }

    @Test fun `every supported payment method has a readable English and Urdu label`() {
        val labels = mapOf("cash" to "Cash", "bank" to "Bank transfer", "easypaisa" to "Easypaisa", "jazzcash" to "JazzCash", "other" to "Other")
        labels.forEach { (method, label) ->
            assertEquals(label, paymentMethodLabel(method, false))
            assertTrue(paymentMethodLabel(method, true).any { it in '\u0600'..'\u06ff' })
        }
    }

    @Test fun `plan frequencies translate consistently and biweekly means every two weeks`() {
        assertEquals("Every two weeks", frequencyLabel("biweekly", false))
        assertEquals("ہر دو ہفتے", frequencyLabel("biweekly", true))
        assertEquals("ہفتہ وار", frequencyLabel("weekly", true))
        assertEquals("ماہانہ", frequencyLabel("monthly", true))
    }

    @Test fun `unknown server labels retain their original content`() {
        assertEquals("Custom method", paymentMethodLabel("Custom method", true))
        assertEquals("Custom frequency", frequencyLabel("Custom frequency", true))
    }
}
