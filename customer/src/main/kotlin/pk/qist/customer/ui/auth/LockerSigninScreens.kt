package pk.qist.customer.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pk.qist.customer.R
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.InputBorder
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

/** Uses the original Qist form layout instead of introducing another visual system. */
@Composable
internal fun SigninMethodsScreen(
    message: String?,
    onPhone: () -> Unit,
    onCode: () -> Unit,
    onPassword: () -> Unit,
    onLanguage: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().background(Canvas).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(QistSpacing.AuthScreen),
        verticalArrangement = Arrangement.spacedBy(QistSpacing.Item),
    ) {
        TextButton(onClick = onLanguage, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
            Text(copy("اردو", "English"))
        }
        Spacer(Modifier.height(16.dp))
        Text(copy("Welcome back", "واپس خوش آمدید"), style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() })
        Text(copy("Choose how to access your installment plan.", "اپنے قسطوں کے منصوبے میں داخل ہونے کا طریقہ چنیں۔"),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Pill(copy("Continue with this phone", "اسی فون سے جاری رکھیں"), onPhone)
        OutlinedButton(onClick = onCode, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = QistShapes.Input) {
            Icon(painterResource(R.drawable.detail_bell), null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(copy("Get a code on your financed phone", "اپنے قسط والے فون پر کوڈ لیں"))
        }
        OutlinedButton(onClick = onPassword, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = QistShapes.Input) {
            Icon(painterResource(R.drawable.detail_lock), null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(copy("Use your password", "اپنا پاس ورڈ استعمال کریں"))
        }
        AuthError(message)
        Spacer(Modifier.height(16.dp))
        Text(copy("Only customers registered by their retailer can sign in.",
            "صرف دکاندار کے رجسٹرڈ صارفین لاگ ان کر سکتے ہیں۔"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}

/** Keep the new device-verification logic, but render it in the existing AuthScreen. */
@Composable
internal fun PhoneVerificationProgress(name: String?, stage: Int, onBack: () -> Unit, onCode: () -> Unit) {
    AuthScreen(
        title = if (name.isNullOrBlank()) copy("Welcome back", "واپس خوش آمدید")
            else copy("Welcome back, $name", "واپس خوش آمدید، $name"),
        subtitle = copy("Confirming this phone with Qist Locker.",
            "Qist Locker سے اس فون کی تصدیق ہو رہی ہے۔"),
        action = copy("Use a code instead", "اس کی بجائے کوڈ استعمال کریں"),
        onBack = onBack,
        onAction = onCode,
    ) {
        val steps = listOf(
            copy("Qist Locker found on this phone", "اس فون پر Qist Locker موجود ہے"),
            copy("Phone matches your plan", "فون آپ کے منصوبے سے ملتا ہے"),
            copy("Signing you in", "آپ کو لاگ ان کر رہے ہیں"),
        )
        steps.forEachIndexed { index, label ->
            Surface(
                shape = QistShapes.Card, color = Color.White,
                border = BorderStroke(1.dp, InputBorder),
                modifier = Modifier.fillMaxWidth().padding(bottom = QistSpacing.Item),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        stage > index -> Icon(painterResource(R.drawable.detail_check), null,
                            tint = Blue, modifier = Modifier.size(28.dp))
                        stage == index -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        else -> Icon(painterResource(R.drawable.detail_clock), null,
                            tint = InputBorder, modifier = Modifier.size(28.dp))
                    }
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
