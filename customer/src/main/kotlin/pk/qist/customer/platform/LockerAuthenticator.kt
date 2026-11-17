package pk.qist.customer.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import pk.qist.customer.data.LockerAuthenticator
import pk.qist.device.auth.ICustomerSignin
import pk.qist.device.auth.ICustomerSigninCallback

/** Explicit, signature-matched Binder IPC. Credentials never travel through an intent or callback. */
internal class AndroidLockerAuthenticator(context: Context) : LockerAuthenticator {
    private val context = context.applicationContext
    private val component = ComponentName(LOCKER, "pk.qist.device.auth.CustomerSigninService")

    override suspend fun available(): Boolean = withContext(Dispatchers.IO) {
        val packages = context.packageManager
        packages.checkSignatures(context.packageName, LOCKER) == PackageManager.SIGNATURE_MATCH &&
            packages.resolveService(Intent().setComponent(component), 0)?.serviceInfo?.let {
                it.exported && it.packageName == LOCKER && it.permission == PERMISSION
            } == true
    }

    override suspend fun authenticate(requestId: String, nonce: String): Boolean {
        if (!available()) return false
        return withContext(Dispatchers.Main.immediate) {
            var connection: ServiceConnection? = null
            var bound = false
            try {
                withTimeoutOrNull(35_000) {
                    suspendCancellableCoroutine { continuation ->
                        val completed = AtomicBoolean(false)
                        fun finish(approved: Boolean) {
                            if (completed.compareAndSet(false, true) && continuation.isActive) continuation.resume(approved)
                        }
                        fun fail(error: Exception) {
                            if (completed.compareAndSet(false, true) && continuation.isActive) continuation.resumeWithException(error)
                        }
                        val callback = object : ICustomerSigninCallback.Stub() {
                            override fun onResult(approved: Boolean, error: String?) {
                                finish(approved)
                            }
                        }
                        connection = object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                                if (!continuation.isActive) return
                                if (name != component) {
                                    fail(SecurityException("Unexpected sign-in service"))
                                    return
                                }
                                try {
                                    ICustomerSignin.Stub.asInterface(binder).authenticate(requestId, nonce, callback)
                                } catch (_: android.os.RemoteException) {
                                    finish(false)
                                } catch (_: SecurityException) {
                                    finish(false)
                                }
                            }
                            override fun onServiceDisconnected(name: ComponentName) {
                                fail(IOException("Locker disconnected. Try again or use recovery."))
                            }
                            override fun onBindingDied(name: ComponentName) {
                                finish(false)
                            }
                            override fun onNullBinding(name: ComponentName) {
                                finish(false)
                            }
                        }
                        try {
                            bound = context.bindService(Intent().setComponent(component), requireNotNull(connection), Context.BIND_AUTO_CREATE)
                            if (!bound) finish(false)
                        } catch (_: SecurityException) {
                            finish(false)
                        }
                    }
                } ?: false
            } finally {
                if (bound) connection?.let { context.unbindService(it) }
            }
        }
    }

    private companion object {
        const val LOCKER = "pk.qist.device"
        const val PERMISSION = "pk.qist.device.permission.CUSTOMER_AUTH"
    }
}
