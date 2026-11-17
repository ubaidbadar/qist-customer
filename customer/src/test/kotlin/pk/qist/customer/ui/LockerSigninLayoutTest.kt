package pk.qist.customer.ui

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pk.qist.customer.ui.auth.SigninMethodsScreen
import pk.qist.customer.ui.layout.CustomerViewport
import pk.qist.customer.ui.theme.QistTheme

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockerSigninLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun `original welcome stays usable at double font size`() {
        var continued = 0
        compose.setContent {
            QistTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    CustomerViewport(authentication = true) {
                        SigninMethodsScreen(null, { continued++ }, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Your installments, always clear.").assertExists()
        compose.onNodeWithText("Get started").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, continued) }
    }
}
