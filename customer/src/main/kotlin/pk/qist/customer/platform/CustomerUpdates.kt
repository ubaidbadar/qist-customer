package pk.qist.customer.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.play.core.appupdate.*
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.*
import kotlinx.coroutines.launch
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy

internal val LocalCheckForUpdate = staticCompositionLocalOf<() -> Unit> { {} }

/** Flexible Play updates: download while using the app; install/restart only after explicit consent. */
@Composable
internal fun CustomerUpdateHost(enabled: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val manager = remember(context) { AppUpdateManagerFactory.create(context.applicationContext) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var offered by rememberSaveable { mutableStateOf(false) }
    var installOffered by rememberSaveable { mutableStateOf(false) }
    var info by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var ready by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val enabledNow by rememberUpdatedState(enabled)
    val unavailable = copy("Update check is unavailable. You can keep using Qist.", "اپ ڈیٹ چیک نہیں ہو سکی۔ آپ قسط استعمال جاری رکھ سکتے ہیں۔")
    val latest = copy("You are up to date.", "آپ کا ورژن تازہ ہے۔")
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { /* Cancellation never blocks the app. */ }
    fun check(manual: Boolean = false) {
        if (checking) return
        checking = true
        manager.appUpdateInfo.addOnSuccessListener { result ->
            checking = false
            when {
                result.installStatus() == InstallStatus.DOWNLOADED && (!installOffered || manual) -> { installOffered = true; ready = true }
                result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && (!offered || manual) -> { offered = true; info = result }
                manual -> scope.launch { snackbar.accountMessage(latest) }
            }
        }.addOnFailureListener { checking = false; if (manual) scope.launch { snackbar.accountMessage(unavailable, error = true) } }
    }
    DisposableEffect(manager, lifecycle) {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) { installOffered = true; ready = true }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME && enabledNow) check() }
        manager.registerListener(listener); lifecycle.addObserver(observer)
        onDispose { manager.unregisterListener(listener); lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(enabled) { if (enabled) check() }
    CompositionLocalProvider(LocalCheckForUpdate provides { check(true) }) {
        Box(Modifier.fillMaxSize()) {
            content()
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp)) { CustomerSnackbarHost(snackbar) }
        }
    }
    info?.let { update ->
        AlertDialog(onDismissRequest = { info = null },
            title = { Text(copy("Update available", "اپ ڈیٹ دستیاب ہے")) },
            text = { Text(copy("Download the latest Qist while you continue using the app.", "ایپ استعمال کرتے ہوئے قسط کا نیا ورژن ڈاؤن لوڈ کریں۔")) },
            confirmButton = { TextButton(onClick = {
                info = null
                try { if (!manager.startUpdateFlowForResult(update, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())) scope.launch { snackbar.accountMessage(unavailable, error = true) } }
                catch (_: android.content.IntentSender.SendIntentException) { scope.launch { snackbar.accountMessage(unavailable, error = true) } }
            }) { Text(copy("Update", "اپ ڈیٹ کریں")) } },
            dismissButton = { TextButton(onClick = { info = null }) { Text(copy("Later", "بعد میں")) } })
    }
    if (ready) AlertDialog(onDismissRequest = { ready = false }, title = { Text(copy("Update ready", "اپ ڈیٹ تیار ہے")) },
        text = { Text(copy("Restart Qist to finish installing. You can also do this later.", "انسٹال مکمل کرنے کے لیے قسط دوبارہ شروع کریں۔ یہ بعد میں بھی کر سکتے ہیں۔")) },
        confirmButton = { TextButton(onClick = { ready = false; manager.completeUpdate().addOnFailureListener { scope.launch { snackbar.accountMessage(unavailable, error = true) } } }) { Text(copy("Restart", "دوبارہ شروع کریں")) } },
        dismissButton = { TextButton(onClick = { ready = false }) { Text(copy("Later", "بعد میں")) } })
}
