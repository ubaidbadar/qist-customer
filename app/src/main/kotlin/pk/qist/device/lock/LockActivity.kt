package pk.qist.device.lock

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import pk.qist.device.ui.theme.QistTheme

/**
 * The full-screen lock. While locked it runs in kiosk (lock-task) mode, so Home, Recents and the notification
 * shade are gone and only the dialer can open. It watches the stored state and closes itself once unlocked.
 * Kept small on purpose: it is the first screen after a reboot of a locked phone.
 */
class LockActivity : ComponentActivity() {
    private val viewModel: LockViewModel by viewModels { LockViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // Back does nothing here; in kiosk mode there is nowhere to go back to.
        onBackPressedDispatcher.addCallback(this) {}

        setContent {
            QistTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                (state as? LockUiState.Locked)?.let { locked ->
                    LockScreenContent(
                        screen = locked.screen,
                        onCallBranch = { phone -> openDialer(phone) },
                        onEmergencyCall = { openDialer() }
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.uiState.collect(::follow) }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    private fun follow(state: LockUiState) = when (state) {
        is LockUiState.Locked -> enterKiosk()
        LockUiState.Unlocked -> leave()
        LockUiState.Loading -> Unit
    }

    private fun enterKiosk() {
        val permitted = getSystemService(DevicePolicyManager::class.java).isLockTaskPermitted(packageName)
        if (permitted && !inKiosk()) startLockTask()
    }

    private fun leave() {
        if (inKiosk()) stopLockTask()
        finish()
    }

    private fun inKiosk() =
        getSystemService(ActivityManager::class.java).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
}
