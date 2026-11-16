package pk.qist.customer.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pk.qist.customer.ui.layout.CustomerViewport
import pk.qist.customer.ui.payments.PaymentSummaryCards
import pk.qist.customer.ui.theme.QistTheme

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi")
    fun `narrow windows stack summary cards and preserve large accessible text`() {
        compose.setContent {
            QistTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    CustomerViewport(authentication = false) {
                        PaymentSummaryCards(paid = { Text("Rs. 1,250,000", Modifier.testTag("paid")) }, remaining = { Text("Rs. 950,000", Modifier.testTag("remaining")) })
                    }
                }
            }
        }
        val paid = compose.onNodeWithTag("payments-paid-card").fetchSemanticsNode().boundsInRoot
        val remaining = compose.onNodeWithTag("payments-remaining-card").fetchSemanticsNode().boundsInRoot
        assertTrue(remaining.top >= paid.bottom)
        for (tag in listOf("paid", "remaining")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(tag).assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty())
            val layout = layouts.single()
            assertFalse("$tag: size=${layout.size}, lines=${layout.lineCount}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}, constraints=${layout.layoutInput.constraints}", layout.hasVisualOverflow)
        }
    }

    @Test @Config(qualifiers = "w1200dp-h1000dp-mdpi")
    fun `tablet content is centered and bounded instead of stretching phone cards`() {
        compose.setContent { QistTheme { CustomerViewport(authentication = false) { Box(Modifier.fillMaxSize().testTag("content")) } } }
        val content = compose.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
        assertTrue(content.width <= 840.5f)
        assertTrue(content.left > 0f)
    }

    @Test @Config(qualifiers = "w393dp-h850dp-mdpi")
    fun `normal phone keeps side by side cards`() {
        compose.setContent { QistTheme { CustomerViewport(authentication = false) { PaymentSummaryCards(paid = { Text("Paid") }, remaining = { Text("Remaining") }) } } }
        val paid = compose.onNodeWithTag("payments-paid-card").fetchSemanticsNode().boundsInRoot
        val remaining = compose.onNodeWithTag("payments-remaining-card").fetchSemanticsNode().boundsInRoot
        assertTrue(remaining.left >= paid.right)
        compose.onNodeWithText("Paid").assertIsDisplayed()
    }
}
