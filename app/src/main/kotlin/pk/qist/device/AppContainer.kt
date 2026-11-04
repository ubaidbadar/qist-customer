package pk.qist.device

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import pk.qist.device.checkin.CheckIn
import pk.qist.device.checkin.CommandRunner
import pk.qist.device.data.DeviceApi
import pk.qist.device.data.DeviceState
import pk.qist.device.data.DeviceStateSerializer
import pk.qist.device.data.HttpDeviceApi
import pk.qist.device.data.KeystoreSealer
import pk.qist.device.deviceadmin.DevicePolicy
import pk.qist.device.deviceadmin.DpmDevicePolicy
import pk.qist.device.deviceadmin.QistAdminReceiver
import pk.qist.device.enrolment.Enroller

/**
 * Builds the app's objects by hand and hands them to whoever needs them (constructor injection, no framework).
 * Everything is `by lazy`, so the lock screen's cold start only creates the state store.
 */
class AppContainer(private val context: Context) {
    val io = Dispatchers.IO

    /** Lives as long as the process; used by broadcast receivers that must finish async work. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val appInfo = AppInfo(appVersion = BuildConfig.VERSION_NAME, androidVersion = Build.VERSION.RELEASE)

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val store: DataStore<DeviceState> by lazy {
        DataStoreFactory.create(
            serializer = DeviceStateSerializer(KeystoreSealer(alias = "qist_state"), json),
            // A state file that cannot be decrypted (key lost) starts over rather than crashing every start.
            corruptionHandler = ReplaceFileCorruptionHandler { DeviceState() },
            scope = CoroutineScope(io + SupervisorJob()),
            produceFile = { File(context.noBackupFilesDir, "device_state.bin") }
        )
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: DeviceApi by lazy { HttpDeviceApi(httpClient, BuildConfig.API_URL.toHttpUrl(), json, io) }

    val policy: DevicePolicy by lazy {
        DpmDevicePolicy(
            context = context,
            dpm = context.getSystemService(DevicePolicyManager::class.java),
            admin = QistAdminReceiver.component(context),
            blockUsbDebugging = BuildConfig.BLOCK_USB_DEBUGGING
        )
    }

    val scheduler: WorkScheduler by lazy { WorkManagerScheduler { WorkManager.getInstance(context) } }

    val enroller by lazy { Enroller(api, store, policy, scheduler, appInfo) }

    val checkIn by lazy { CheckIn(api, store, policy, CommandRunner(store, policy), scheduler, appInfo) }
}
