package pk.qist.customer.ui

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pk.qist.customer.ui.auth.SigninMethodsScreen
import pk.qist.customer.ui.auth.Welcome
import pk.qist.customer.ui.layout.CustomerViewport
import pk.qist.customer.ui.theme.QistTheme

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockerSigninLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun `all sign in methods remain reachable at double font size on a small phone`() {
        var selected = ""
        compose.setContent {
            QistTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    CustomerViewport(authentication = true) {
                        SigninMethodsScreen(null, { selected = "local" }, { selected = "code" }, { selected = "password" })
                    }
                }
            }
        }
        for ((title, expected) in listOf("Continue with this phone" to "local", "Get a code on your financed phone" to "code", "Use your password" to "password")) {
            val node = compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse(layouts.single().didOverflowHeight)
            assertFalse(layouts.single().didOverflowWidth)
            node.performClick(); assertEquals(expected, selected)
        }
    }
    @Test @Config(qualifiers = "w393dp-h852dp-mdpi")
    fun `original Qist welcome remains visible and continues to sign in`() {
        var started = 0
        compose.setContent {
            QistTheme {
                CustomerViewport(authentication = true) {
                    Welcome(onLanguage = {}, onContinue = { started++ })
                }
            }
        }
        compose.onNodeWithText("Qist").assertIsDisplayed()
        compose.onNodeWithText("Your installments, always clear.").assertIsDisplayed()
        compose.onNodeWithText("Get started").performClick()
        compose.runOnIdle { assertEquals(1, started) }
    }

}
