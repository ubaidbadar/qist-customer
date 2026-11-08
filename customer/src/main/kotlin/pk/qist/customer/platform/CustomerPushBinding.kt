package pk.qist.customer.platform

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.drop

import pk.qist.customer.data.CustomerPreferences
import pk.qist.customer.ui.CustomerViewModel

/** No polling: pushes refresh the authenticated overview; foreground entry repairs missed delivery. */
@Composable
internal fun CustomerPushBinding(signedIn: Boolean, preferences: CustomerPreferences, model: CustomerViewModel) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val enabled by preferences.notificationPreference.collectAsState()
    val token by CustomerPushSignals.token.collectAsState()
    var resumed by remember { mutableIntStateOf(0) }
    var previouslyEnabled by remember { mutableStateOf(enabled) }
    fun permissionGranted() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(permissionGranted()) }
    var permissionPending by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionPending = false
        granted = it
        if (!it) preferences.setNotifications(false) else resumed++
    }
    val currentSignedIn by rememberUpdatedState(signedIn)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = permissionGranted()
                resumed++
                if (currentSignedIn) model.onForeground()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(signedIn) {
        preferences.setCustomerActive(signedIn)
        if (!signedIn) { (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll() }
    }
    LaunchedEffect(signedIn, enabled, granted, permissionPending) {
        val explicitOptIn = enabled && !previouslyEnabled
        previouslyEnabled = enabled
        if (signedIn && !permissionPending) when (notificationPermissionAction(enabled, explicitOptIn, granted)) {
            NotificationPermissionAction.Ask -> { permissionPending = true; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
            NotificationPermissionAction.TurnOff -> preferences.setNotifications(false)
            NotificationPermissionAction.Keep -> Unit
        }
    }
    LaunchedEffect(signedIn, enabled, granted, resumed) {
        val active = signedIn && enabled && granted
        FirebaseMessaging.getInstance().isAutoInitEnabled = active
        if (active) FirebaseMessaging.getInstance().token.addOnSuccessListener { CustomerPushSignals.token.value = it }
            .addOnFailureListener { model.pushUnavailable() }
    }
    LaunchedEffect(signedIn, enabled, granted, token, resumed) {
        if (!signedIn || (enabled && !granted)) return@LaunchedEffect
        if (!enabled && !preferences.hasNotificationPreference) return@LaunchedEffect
        val desired = if (enabled) token ?: return@LaunchedEffect else null
        model.syncPushToken(desired)
    }
    LaunchedEffect(signedIn, lifecycle) {
        if (signedIn) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            CustomerPushSignals.revision.drop(1).collect { model.onRealtimeChange() }
        }
    }
}
