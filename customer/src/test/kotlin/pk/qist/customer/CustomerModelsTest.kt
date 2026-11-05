package pk.qist.customer

import org.junit.Assert.*
import org.junit.Test
import pk.qist.customer.data.*
import pk.qist.customer.ui.auth.normalizePhone

class CustomerModelsTest {
    @Test fun moneyUsesPaisaWithoutFloatingPointRounding() {
        assertEquals("7,500", formatAmount(750000))
        assertEquals("7,500.25", formatAmount(750025))
        assertEquals("0.01", formatAmount(1))
    }
    @Test fun remainingIncludesFeesAndActualPartPayments() {
        assertEquals(65000L, CustomerInstallment(1, "2026-11-05", 100000, 5000, 40000, "partial").remaining)
        assertEquals(0L, CustomerInstallment(1, "2026-11-05", 100000, 0, 100001, "paid").remaining)
    }
    @Test fun phoneNormalizationHandlesLocalInternationalAndUrduDigits() {
        assertEquals("3001234567", normalizePhone("+92 300 1234567"))
        assertEquals("3001234567", normalizePhone("0300-1234567"))
        assertEquals("3001234567", normalizePhone("۰۳۰۰۱۲۳۴۵۶۷"))
    }
    @Test fun dueDaysUseServerCalendarDateAndUtc() {
        assertEquals(1, daysUntil("2028-03-01", "2028-02-29"))
        assertEquals(-1, daysUntil("2026-11-04", "2026-11-05"))
        assertNull(daysUntil("invalid", "2026-11-05"))
    }
}
