package pk.qist.device.lock

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pk.qist.device.data.DeviceState
import pk.qist.device.data.LockScreen
import pk.qist.device.data.LockState

class LockScreenFormatterTest {
    @Test
    fun `rupees group thousands and drop zero paisa`() {
        assertEquals("Rs 0", LockScreenFormatter.rupees(0))
        assertEquals("Rs 500", LockScreenFormatter.rupees(500_00))
        assertEquals("Rs 15,000", LockScreenFormatter.rupees(15_000_00))
        assertEquals("Rs 1,234,567", LockScreenFormatter.rupees(1_234_567_00))
    }

    @Test
    fun `rupees keep paisa as two digits`() {
        assertEquals("Rs 15,000.50", LockScreenFormatter.rupees(15_000_50))
        assertEquals("Rs 1.05", LockScreenFormatter.rupees(105))
    }

    @Test
    fun `dates read the way receipts write them`() {
        assertEquals("12 Oct 2026", LockScreenFormatter.date("2026-10-12"))
        assertEquals("1 Jan 2027", LockScreenFormatter.date("2027-01-01"))
    }

    @Test
    fun `malformed dates are left out rather than shown wrong`() {
        assertNull(LockScreenFormatter.date("2026-13-01"))
        assertNull(LockScreenFormatter.date("12/10/2026"))
        assertNull(LockScreenFormatter.date(""))
    }

    @Test
    fun `a full lock screen formats every part`() {
        val ui = LockScreenFormatter.format(
            LockScreen(
                amountDue = 15_000_00,
                nextDueDate = "2026-10-12",
                branchName = "Saddar",
                branchPhone = "0300 1234567",
                message = "Please pay"
            ),
            now = Date(0)
        )

        assertEquals(LockScreenUi("Please pay", "Rs 15,000", "12 Oct 2026", "Saddar", "0300 1234567"), ui)
    }

    @Test
    fun `overdue days follow the Pakistan calendar date`() {
        val screen = LockScreen(amountDue = 100, nextDueDate = "1970-01-01")
        assertNull(LockScreenFormatter.format(screen, now = Date(68_399_999)).overdueDays)
        assertEquals(1, LockScreenFormatter.format(screen, now = Date(68_400_000)).overdueDays)
        assertNull(LockScreenFormatter.format(screen.copy(amountDue = 0), now = Date(68_400_000)).overdueDays)
        assertNull(LockScreenFormatter.format(screen.copy(nextDueDate = "1970-02-31"), now = Date(68_400_000)).overdueDays)
    }

    @Test
    fun `missing parts are left out and a blank message gets a calm default`() {
        val ui = LockScreenFormatter.format(LockScreen())

        assertNull(ui.amountDue)
        assertNull(ui.nextDueDate)
        assertNull(ui.branchName)
        assertNull(ui.branchPhone)
        assertEquals(true, ui.message.startsWith("This phone is locked"))
    }

    @Test
    fun `the lock screen shows only while the stored state is locked`() {
        assertEquals(LockUiState.Unlocked, DeviceState().toLockUiState())
        val locked = DeviceState(lockState = LockState.LOCKED, lockScreen = LockScreen(amountDue = 100))
        assertEquals("Rs 1", (locked.toLockUiState() as LockUiState.Locked).screen.amountDue)
    }
}
