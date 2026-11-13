package pk.qist.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.qist.customer.ui.CustomerTab
import pk.qist.customer.ui.auth.*
import pk.qist.customer.ui.components.panelSection
import pk.qist.customer.ui.dashboard.CustomerScaffold
import pk.qist.customer.ui.theme.QistTheme

/** Real UI assertions; run on an emulator/device with :customer:connectedDebugAndroidTest. */
@RunWith(AndroidJUnit4::class)
class CustomerUiRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun passwordIsMaskedAndVisibilityDoesNotSurviveRecreation() {
        val restore = StateRestorationTester(compose)
        restore.setContent { QistTheme { PasswordField("private-password") {} } }
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithContentDescription("Show password").performClick()
        compose.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Show password").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test fun pastedOtpNormalizesDigitsAndStopsAtSix() {
        var observed = ""
        compose.setContent { QistTheme {
            var code by remember { mutableStateOf("") }
            CodeField(code) { code = it; observed = it }
        } }
        compose.onNodeWithContentDescription("Six-digit verification code").performTextInput("١٢٣٤٥٦٧")
        compose.runOnIdle { assertEquals("123456", observed) }
    }

    @Test fun lockoutDisablesInputAndVerificationUntilCleared() {
        var blockedUntil by mutableLongStateOf(60_000)
        compose.setContent { QistTheme {
            VerificationScreen("3001234567", "123456", false, null, false, 0, blockedUntil, {}, {}, {}, {}, clock = { compose.mainClock.currentTime })
        } }
        compose.onNodeWithContentDescription("Six-digit verification code").assertIsNotEnabled()
        compose.onNodeWithText("Verify").assertIsNotEnabled()
        compose.runOnIdle { blockedUntil = 0 }
        compose.onNodeWithContentDescription("Six-digit verification code").assertIsEnabled()
        compose.onNodeWithText("Verify").assertIsEnabled()
    }

    @Test fun navigationChangesTheActiveTabWithoutRenderingOtherTabs() {
        compose.setContent { QistTheme {
            var selected by remember { mutableStateOf(CustomerTab.Home) }
            CustomerScaffold(selected, { selected = it }) { padding ->
                Text("Content: ${selected.name}", Modifier.padding(padding))
            }
        } }
        compose.onNodeWithText("Content: Home").assertIsDisplayed()
        compose.onNodeWithText("Plan").performClick()
        compose.onNodeWithText("Content: Plan").assertIsDisplayed()
        compose.onNodeWithText("Content: Home").assertDoesNotExist()
        compose.onNodeWithText("Plan").assertIsSelected()
    }

    @Test fun distantLazyRowsAreCreatedWhenScrolledIntoView() {
        compose.setContent { QistTheme {
            LazyColumn(Modifier.height(220.dp).testTag("rows")) {
                panelSection("test", (0 until 200).toList(), { it.toString() }, title = { Text("Schedule") }) {
                    Text("Row $it", Modifier.padding(vertical = 14.dp))
                }
            }
        } }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
        compose.onNodeWithText("Row 199").assertDoesNotExist()
        compose.onNodeWithTag("rows").performScrollToNode(hasText("Row 199"))
        compose.onNodeWithText("Row 199").assertIsDisplayed()
    }
}
