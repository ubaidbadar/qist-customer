package pk.qist.customer.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pk.qist.customer.R
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.InputBorder
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

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
