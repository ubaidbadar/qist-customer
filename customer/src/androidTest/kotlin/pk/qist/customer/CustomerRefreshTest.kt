package pk.qist.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.qist.customer.ui.components.CustomerRefreshBox
import pk.qist.customer.ui.theme.QistTheme

/** Covers the widget's gesture/lifecycle state, which ViewModel tests cannot exercise. */
@RunWith(AndroidJUnit4::class)
class CustomerRefreshTest {
    @get:Rule val compose = createComposeRule()

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Composable
    private fun Content(refreshing: Boolean = false, revision: Int = 0, onRefresh: () -> Unit) {
        CustomerRefreshBox(refreshing, onRefresh, Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().testTag("refresh-content").verticalScroll(rememberScrollState())) {
                Text("Account revision $revision")
                Spacer(Modifier.height(1600.dp))
            }
        }
    }

    private fun beginInterruptedPull() {
        compose.onNodeWithTag("refresh-content").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, center.y * 0.4f))
        }
        compose.onNodeWithTag("customer-refresh-indicator").assertExists()
    }

    @Test fun contentUpdatesDoNotCreateAnIdleIndicatorOrRequestRefresh() {
        var revision by mutableIntStateOf(0)
        var requests = 0
        compose.setContent { QistTheme { Content(revision = revision) { requests++ } } }
        repeat(20) {
            compose.runOnIdle { revision++ }
            compose.onNodeWithTag("customer-refresh-indicator").assertDoesNotExist()
        }
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test fun activityRestorationDoesNotRestoreAnInterruptedPullPosition() {
        var requests = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent { QistTheme { Content { requests++ } } }
        beginInterruptedPull()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("customer-refresh-indicator").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test fun interruptedPullIsHiddenAndClearedAcrossPauseAndResume() {
        lateinit var owner: Owner
        var requests = 0
        compose.setContent { QistTheme {
            val lifecycleOwner = remember { Owner().apply { registry.currentState = Lifecycle.State.RESUMED } }
            owner = lifecycleOwner
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                Content { requests++ }
            }
        } }
        beginInterruptedPull()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.onNodeWithTag("customer-refresh-indicator").assertDoesNotExist()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.onNodeWithTag("customer-refresh-indicator").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test fun aManualPullRequestsRefreshAndShowsTheIndicatorUntilCompletion() {
        var refreshing by mutableStateOf(false)
        var requests = 0
        compose.setContent { QistTheme { Content(refreshing) { requests++; refreshing = true } } }
        compose.onNodeWithTag("refresh-content").performTouchInput { swipeDown() }
        compose.onNodeWithTag("customer-refresh-indicator").assertExists()
        compose.runOnIdle { assertEquals(1, requests); refreshing = false }
        compose.onNodeWithTag("customer-refresh-indicator").assertDoesNotExist()
    }
}
