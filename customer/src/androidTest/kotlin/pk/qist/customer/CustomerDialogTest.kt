package pk.qist.customer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.qist.customer.ui.components.CustomerDialog
import pk.qist.customer.ui.theme.QistTheme

@RunWith(AndroidJUnit4::class)
class CustomerDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmationAndCancelHaveIndependentActions() {
        var confirmed = 0
        var dismissed = 0
        compose.setContent { QistTheme {
            CustomerDialog("Delete your data?", "Your retailer records remain.", "Delete", { confirmed++ }, { dismissed++ },
                R.drawable.detail_delete, dismissLabel = "Cancel", destructive = true)
        } }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, confirmed); assertEquals(1, dismissed) }
        compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle { assertEquals(1, confirmed); assertEquals(1, dismissed) }
    }

    @Test fun busyDialogDisablesActionsAndKeepsItsErrorVisible() {
        compose.setContent { QistTheme {
            CustomerDialog("Delete your data?", "Your retailer records remain.", "Deleting…", {}, {},
                R.drawable.detail_delete, dismissLabel = "Cancel", destructive = true, busy = true, error = "Try again later")
        } }
        compose.onNodeWithText("Deleting…").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
        compose.onNodeWithText("Try again later").assertIsDisplayed()
    }

    @Test fun oneActionDialogSupportsLargeTextWithoutAnExtraCancelButton() {
        compose.setContent { QistTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.6f)) {
                CustomerDialog("Contact retailer", "The shop has not provided a phone number.", "Close", {}, {}, R.drawable.payment_call)
            }
        } }
        compose.onNodeWithText("Close").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Cancel").assertDoesNotExist()
    }
}
