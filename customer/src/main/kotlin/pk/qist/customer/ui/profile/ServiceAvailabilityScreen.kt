package pk.qist.customer.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.ServiceGate
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun ServiceAvailabilityScreen(gate: ServiceGate, busy: Boolean, error: String?, onRetry: () -> Unit) {
    val update = gate == ServiceGate.UpdateRequired
    val context = LocalContext.current
    val unavailable = copy("No app is available to open Google Play.", "گوگل پلے کھولنے کے لیے کوئی ایپ دستیاب نہیں۔")
    fun openStore() {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending")) }
        catch (_: ActivityNotFoundException) {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            catch (_: ActivityNotFoundException) { Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show() }
        }
    }
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenBadge(if (update) R.drawable.state_download else R.drawable.state_maintenance,
            if (update) Blue else DueAmber, if (update) BlueBackground else DueBackground, 88)
        Spacer(Modifier.height(24.dp))
        Text(if (update) copy("Update required", "اپ ڈیٹ ضروری ہے") else copy("We will be back soon", "ہم جلد واپس آئیں گے"),
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(if (update) copy("A new version of Qist is available. Please update to continue.", "قسط کا نیا ورژن دستیاب ہے۔ جاری رکھنے کے لیے اپ ڈیٹ کریں۔")
            else copy("Qist is under maintenance. Please try again in a little while.", "قسط کی دیکھ بھال جاری ہے۔ کچھ دیر بعد دوبارہ کوشش کریں۔"),
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        if (update) Button(onClick = ::openStore, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Text(copy("Update on Google Play", "گوگل پلے پر اپ ڈیٹ کریں"))
        }
        OutlinedButton(onClick = onRetry, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(copy("Try again", "دوبارہ کوشش کریں"))
        }
        if (error != null) Text(error, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}
