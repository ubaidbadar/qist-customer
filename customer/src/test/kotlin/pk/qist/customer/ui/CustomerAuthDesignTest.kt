package pk.qist.customer.ui

import android.app.Application
import android.os.SystemClock
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pk.qist.customer.R
import pk.qist.customer.ui.auth.AuthDivider
import pk.qist.customer.ui.auth.AuthScreen
import pk.qist.customer.ui.auth.AuthSecondaryAction
import pk.qist.customer.ui.auth.PasswordField
import pk.qist.customer.ui.auth.PhoneField
import pk.qist.customer.ui.auth.VerificationScreen
import pk.qist.customer.ui.layout.CustomerViewport
import pk.qist.customer.ui.theme.QistTheme

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomerAuthDesignTest {
    @get:Rule val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun `phone sign in follows the reference hierarchy and keeps Locker action visible`() {
        compose.setContent {
            QistTheme {
                CustomerViewport(authentication = true) {
                    AuthScreen(
                        title = "Enter your phone number",
                        subtitle = "Use the number your retailer registered.",
                        action = "Continue",
                        onBack = {},
                        onAction = {},
                        footer = "Qist Lock found on this phone. Not registered? Ask your retailer to add you.",
                        bottomContent = {
                            Spacer(Modifier.height(12.dp))
                            AuthDivider()
                            Spacer(Modifier.height(12.dp))
                            AuthSecondaryAction(
                                label = "Sign in with Qist Lock app",
                                icon = R.drawable.phone_outline,
                                onClick = {},
                            )
                        },
                    ) {
                        PhoneField("3001234567", error = false, onChange = {})
                    }
                }
            }
        }

        compose.onNodeWithText("Enter your phone number").assertIsDisplayed()
        compose.onNodeWithText("Phone number").assertIsDisplayed()
        compose.onNodeWithText("Continue").assertIsDisplayed()
        compose.onNodeWithText("Sign in with Qist Lock app").assertIsDisplayed()
        compose.onNodeWithText("Qist Lock found on this phone. Not registered? Ask your retailer to add you.").assertIsDisplayed()
    }

    @Test
    fun `password field has external label placeholder and accessible visibility control`() {
        compose.setContent {
            QistTheme {
                CustomerViewport(authentication = true) {
                    PasswordField(password = "", onChange = {})
                }
            }
        }

        compose.onNodeWithText("Password").assertIsDisplayed()
        compose.onNodeWithText("Enter your password").assertIsDisplayed()
        compose.onNodeWithContentDescription("Show password").assertIsDisplayed()
    }

    @Test
    fun `verification screen uses the Qist Lock paste action and verify CTA`() {
        compose.setContent {
            QistTheme {
                CustomerViewport(authentication = true) {
                    VerificationScreen(
                        phone = "3001234567",
                        code = "",
                        busy = false,
                        message = null,
                        codeRejected = false,
                        resendAt = SystemClock.elapsedRealtime(),
                        blockedUntil = 0L,
                        onBack = {},
                        onVerify = {},
                        onCode = {},
                        onResend = {},
                        extra = {
                            AuthSecondaryAction(
                                label = "Paste code from Qist Lock",
                                icon = R.drawable.detail_bell,
                                onClick = {},
                            )
                        },
                    )
                }
            }
        }

        compose.onNodeWithText("Enter your code").assertIsDisplayed()
        compose.onNodeWithText("Paste code from Qist Lock").assertIsDisplayed()
        compose.onNodeWithText("Verify").assertIsDisplayed()
    }
}
