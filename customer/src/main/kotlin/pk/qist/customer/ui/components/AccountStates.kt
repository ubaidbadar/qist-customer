package pk.qist.customer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun AccountEmptyState(icon: Int, title: String, description: String, modifier: Modifier = Modifier, actions: @Composable ColumnScope.() -> Unit = {}) {
    Column(modifier.padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).background(BlueBackground, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(40.dp), tint = Blue)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        actions()
    }
}

@Composable
internal fun SessionRestoreLoading() {
    Box(Modifier.fillMaxSize().background(Canvas).systemBarsPadding(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), color = Blue, strokeWidth = 2.dp)
    }
}

@Composable
internal fun SessionExpired(onSignIn: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Canvas).systemBarsPadding()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AccountEmptyState(R.drawable.detail_lock, copy("Session expired", "سیشن ختم ہو گیا"),
                copy("For your security, please sign in again to continue.", "اپنی حفاظت کے لیے دوبارہ لاگ ان کریں۔")) {
                Spacer(Modifier.height(30.dp))
                Pill(copy("Sign in", "لاگ ان کریں"), onSignIn)
            }
        }
    }
}

@Composable
internal fun AccountLoadError(message: String?, busy: Boolean, onRetry: () -> Unit, onSignOut: () -> Unit) {
    var contact by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Canvas).systemBarsPadding()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(96.dp).background(DestructiveBackground, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.state_error), null, Modifier.size(40.dp), tint = Destructive)
            }
            Spacer(Modifier.height(24.dp))
            Text(copy("Something went wrong", "کچھ غلط ہو گیا"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(message ?: copy("We could not load your information. Check your connection and try again.", "آپ کی معلومات لوڈ نہیں ہو سکیں۔ انٹرنیٹ چیک کر کے دوبارہ کوشش کریں۔"),
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(30.dp))
            Button(onClick = onRetry, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = CircleShape) {
                Text(copy("Try again", "دوبارہ کوشش کریں"))
            }
            TextButton(onClick = { contact = true }) { Text(copy("Contact your retailer", "اپنے دکاندار سے رابطہ کریں")) }
            TextButton(onClick = onSignOut, enabled = !busy) { Text(copy("Sign out", "لاگ آؤٹ")) }
        }
    }
    if (contact) RetailerContactDialog { contact = false }
}
