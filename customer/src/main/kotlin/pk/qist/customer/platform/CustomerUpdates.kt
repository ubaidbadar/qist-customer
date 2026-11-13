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
    val downloading = copy("The update is downloading. You can keep using Qist.", "اپ ڈیٹ ڈاؤن لوڈ ہو رہی ہے۔ آپ قسط استعمال کر سکتے ہیں۔")
    val cannotOffer = copy("An update is available, but Google Play cannot offer an in-app download on this device.", "اپ ڈیٹ دستیاب ہے، لیکن اس فون پر ایپ کے اندر ڈاؤن لوڈ ممکن نہیں۔")
    val latest = copy("You are up to date.", "آپ کا ورژن تازہ ہے۔")
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED)
            scope.launch { snackbar.accountMessage(unavailable, error = true) }
    }
    fun check(manual: Boolean = false) {
        if (checking) return
        checking = true
        manager.appUpdateInfo.addOnSuccessListener { result ->
            checking = false
            when {
                result.installStatus() == InstallStatus.DOWNLOADED && (!installOffered || manual) -> { installOffered = true; ready = true }
                result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && (!offered || manual) -> { offered = true; info = result }
                manual && result.installStatus() in listOf(InstallStatus.DOWNLOADING, InstallStatus.PENDING) -> scope.launch { snackbar.accountMessage(downloading) }
                manual && result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> scope.launch { snackbar.accountMessage(cannotOffer, error = true) }
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
    if (ready) CustomerDialog(
        title = copy("Update ready", "اپ ڈیٹ تیار ہے"),
        message = copy("Restart Qist to finish installing. You can also do this later.", "انسٹال مکمل کرنے کے لیے قسط دوبارہ شروع کریں۔ یہ بعد میں بھی کر سکتے ہیں۔"),
        icon = pk.qist.customer.R.drawable.state_download,
        confirmLabel = copy("Restart", "دوبارہ شروع کریں"), dismissLabel = copy("Later", "بعد میں"),
        onDismiss = { ready = false },
        onConfirm = { ready = false; manager.completeUpdate().addOnFailureListener { scope.launch { snackbar.accountMessage(unavailable, error = true) } } },
    ) else info?.let { update ->
        CustomerDialog(
            title = copy("Update available", "اپ ڈیٹ دستیاب ہے"),
            message = copy("Download the latest Qist while you continue using the app.", "ایپ استعمال کرتے ہوئے قسط کا نیا ورژن ڈاؤن لوڈ کریں۔"),
            icon = pk.qist.customer.R.drawable.state_download,
            confirmLabel = copy("Update", "اپ ڈیٹ کریں"), dismissLabel = copy("Later", "بعد میں"),
            onDismiss = { info = null },
            onConfirm = {
                info = null
                try { if (!manager.startUpdateFlowForResult(update, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())) scope.launch { snackbar.accountMessage(unavailable, error = true) } }
                catch (_: android.content.IntentSender.SendIntentException) { scope.launch { snackbar.accountMessage(unavailable, error = true) } }
            },
        )
    }
}
