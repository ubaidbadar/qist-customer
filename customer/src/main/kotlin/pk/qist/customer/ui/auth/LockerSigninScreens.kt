package pk.qist.customer.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pk.qist.customer.R
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.BrandGradientStart
import pk.qist.customer.ui.theme.Muted
import pk.qist.customer.ui.theme.Navy

/**
 * Preserve the original Qist welcome design, while keeping the newer Locker,
 * verification-code, and password sign-in methods available.
 */
@Composable
internal fun SigninMethodsScreen(
    message: String?,
    onContinue: () -> Unit,
    onLanguage: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Navy).systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(
            onClick = onLanguage,
            modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
        ) {
            Text(copy("اردو", "English"), color = Color.White)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(104.dp)
                    .background(Brush.linearGradient(listOf(BrandGradientStart, Blue)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Qist", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            Text(
                copy("Your installments, always clear.", "آپ کی اقساط، ہمیشہ واضح۔"),
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                copy("See your plan, due dates and payments in one place.", "اپنا منصوبہ، آخری تاریخیں اور ادائیگیاں ایک جگہ دیکھیں۔"),
                color = Muted,
                textAlign = TextAlign.Center,
            )
            if (message != null) {
                Spacer(Modifier.height(16.dp))
                AuthError(message)
            }
        }
        Pill(copy("Get started", "شروع کریں"), onContinue, true)
        Spacer(Modifier.height(12.dp))
        Text(
            copy("New here? Your retailer adds you to Qist.", "نئے ہیں؟ آپ کا دکاندار آپ کو قسط میں شامل کرتا ہے۔"),
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Progress is informational; no need for an independent second screen theme or
 * multiple decorative cards. Keep the fallback sign-in action reachable.
 */
@Composable
internal fun PhoneVerificationProgress(
    name: String?,
    stage: Int,
    onBack: () -> Unit,
    onCode: () -> Unit,
) {
    AuthScreen(
        title = if (name.isNullOrBlank()) copy("Welcome back", "واپس خوش آمدید")
            else copy("Welcome back,\n$name", "واپس خوش آمدید،\n$name"),
        subtitle = copy(
            "Confirming this phone with Qist Locker.",
            "Qist Locker سے اس فون کی تصدیق ہو رہی ہے۔",
        ),
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    stage > index -> Icon(
                        painterResource(R.drawable.detail_check),
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(24.dp),
                    )
                    stage == index -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    else -> Box(Modifier.size(24.dp))
                }
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
