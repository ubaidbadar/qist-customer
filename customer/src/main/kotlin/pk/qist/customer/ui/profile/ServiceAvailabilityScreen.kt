package pk.qist.customer.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun ServiceAvailabilityScreen(busy: Boolean, error: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenBadge(R.drawable.state_maintenance, DueAmber, DueBackground, 88)
        Spacer(Modifier.height(24.dp))
        Text(copy("We will be back soon", "ہم جلد واپس آئیں گے"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(copy("Qist is under maintenance. Please try again in a little while.", "قسط کی دیکھ بھال جاری ہے۔ کچھ دیر بعد دوبارہ کوشش کریں۔"),
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        OutlinedButton(onClick = onRetry, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
            Text(copy("Try again", "دوبارہ کوشش کریں"))
        }
        if (error != null) Text(error, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}
