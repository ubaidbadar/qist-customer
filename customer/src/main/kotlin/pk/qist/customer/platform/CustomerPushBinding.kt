package pk.qist.customer.platform

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
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
    var registered by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) preferences.setNotifications(false) else resumed++
    }
    val currentSignedIn by rememberUpdatedState(signedIn)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumed++
                if (currentSignedIn) model.onForeground()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(signedIn) {
        preferences.setCustomerActive(signedIn)
        if (!signedIn) { registered = null; (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll() }
    }
    LaunchedEffect(signedIn, enabled, resumed) {
        if (!signedIn) return@LaunchedEffect
        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return@LaunchedEffect
        }
        FirebaseMessaging.getInstance().isAutoInitEnabled = enabled
        if (enabled) FirebaseMessaging.getInstance().token.addOnSuccessListener { CustomerPushSignals.token.value = it }.addOnFailureListener { model.pushUnavailable() }
        else if (preferences.hasNotificationPreference) { model.syncPushToken(null); registered = null }
    }
    LaunchedEffect(signedIn, enabled, token, resumed) {
        val current = token
        val permitted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (signedIn && enabled && permitted && current != null && current != registered && model.syncPushToken(current)) registered = current
    }
    LaunchedEffect(signedIn, lifecycle) {
        if (signedIn) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            CustomerPushSignals.revision.drop(1).collect { model.onRealtimeChange() }
        }
    }
}
