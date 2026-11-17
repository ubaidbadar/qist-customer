package pk.qist.customer.ui

import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pk.qist.customer.ui.components.CustomerBottomSheet
import pk.qist.customer.ui.theme.QistTheme

/** Host-side verification that the common sheet preserves its screen-owned actions. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomerBottomSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sheetShowsContentAndPassesThroughActions() {
        var selected = 0
        compose.setContent {
            QistTheme {
                CustomerBottomSheet(onDismiss = {}) {
                    Text("Choose an option")
                    Button(onClick = { selected++ }) { Text("Select") }
                }
            }
        }
        compose.onNodeWithText("Choose an option").assertIsDisplayed()
        compose.onNodeWithText("Select").performClick()
        compose.runOnIdle { assertEquals(1, selected) }
    }
}
