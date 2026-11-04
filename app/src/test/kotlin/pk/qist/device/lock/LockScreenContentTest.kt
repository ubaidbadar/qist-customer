package pk.qist.device.lock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.qist.device.ui.theme.QistTheme

/** Runs on the JVM through Robolectric, so it needs no phone or emulator. */
@RunWith(AndroidJUnit4::class)
class LockScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val full = LockScreenUi(
        message = "Please pay your installment",
        amountDue = "Rs 15,000",
        nextDueDate = "12 Oct 2026",
        branchName = "Saddar",
        branchPhone = "0300 1234567",
        overdueDays = 3
    )

    private fun show(screen: LockScreenUi, onCall: (String) -> Unit = {}, onEmergency: () -> Unit = {}) =
        compose.setContent {
            QistTheme { LockScreenContent(screen = screen, onCallBranch = onCall, onEmergencyCall = onEmergency) }
        }

    @Test
    fun `shows the message, amount, due date and branch`() {
        show(full)

        compose.onNodeWithText("Your phone is locked").assertIsDisplayed()
        compose.onNodeWithText("An installment payment is overdue. Please pay your retailer to unlock your phone.")
            .assertIsDisplayed()
        compose.onNodeWithText("Rs 15,000").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("12 Oct 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3 days").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Saddar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `call branch dials the branch number and is labelled for screen readers`() {
        var dialled: String? = null
        show(full, onCall = { dialled = it })

        compose.onNodeWithContentDescription("Call Saddar at 0300 1234567").performScrollTo().performClick()

        assertEquals("0300 1234567", dialled)
    }

    @Test
    fun `emergency call is always there, even without a branch number`() {
        var emergencies = 0
        show(full.copy(branchPhone = null), onEmergency = { emergencies++ })

        compose.onNodeWithText("Call retailer").assertDoesNotExist()
        compose.onNodeWithText("Emergency call").performScrollTo().performClick()

        assertEquals(1, emergencies)
    }

    @Test
    fun `nothing about money shows when nothing is due`() {
        show(full.copy(amountDue = null, nextDueDate = null, overdueDays = null))

        compose.onNodeWithText("Amount due").assertDoesNotExist()
        compose.onNodeWithText("Please pay your installment").assertIsDisplayed()
    }
}
