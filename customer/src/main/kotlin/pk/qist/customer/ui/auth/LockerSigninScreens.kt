package pk.qist.customer.ui.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pk.qist.customer.R
import pk.qist.customer.ui.components.CustomerBackButton
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Canvas

private val AuthNavy = Color(0xFF0B1B40)
private val AuthMuted = Color(0xFFA8B8DA)
private val AuthCyan = Color(0xFF20C7DF)
private val AuthGradient = Brush.linearGradient(listOf(Color(0xFF333BE8), Color(0xFF06B4D0)))

@Composable
internal fun SigninMethodsScreen(message: String?, onPhone: () -> Unit, onCode: () -> Unit, onPassword: () -> Unit, onLanguage: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(AuthNavy).systemBarsPadding().verticalScroll(rememberScrollState())) {
        TextButton(onClick = onLanguage, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) { Text("English / اردو", color = Color.White) }
        AuthHero(R.drawable.detail_lock, copy("Welcome back", "واپس خوش آمدید"),
            copy("Pick the quickest way to get into your installment plan.", "اپنے قسطوں کے منصوبے تک پہنچنے کا آسان طریقہ چنیں۔"))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Canvas)
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SigninChoice(R.drawable.phone_outline, copy("Continue with this phone", "اسی فون سے جاری رکھیں"),
                copy("Qist Locker verifies the account linked to this phone.", "Qist Locker اس فون سے منسلک اکاؤنٹ کی تصدیق کرتا ہے۔"), true, onPhone)
            SigninChoice(R.drawable.detail_bell, copy("Get a code on your financed phone", "اپنے قسط والے فون پر کوڈ لیں"),
                copy("Open the Qist Locker notification to view your code.", "اپنا کوڈ دیکھنے کے لیے Qist Locker کی اطلاع کھولیں۔"), false, onCode)
            SigninChoice(R.drawable.detail_lock, copy("Use your password", "اپنا پاس ورڈ استعمال کریں"),
                copy("Sign in with your phone number and password.", "اپنے فون نمبر اور پاس ورڈ سے لاگ ان کریں۔"), false, onPassword)
            if (message != null) AuthError(message)
            Spacer(Modifier.height(56.dp))
            Text(copy("Only customers your retailer registered can sign in.", "صرف دکاندار کے رجسٹرڈ صارفین لاگ ان کر سکتے ہیں۔"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        }
    }
}

@Composable
private fun SigninChoice(@DrawableRes icon: Int, title: String, detail: String, primary: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(Modifier.fillMaxWidth().clip(shape).then(if (primary) Modifier.background(AuthGradient) else Modifier.background(Color.White).border(1.dp, Color(0xFFDCE3F4), shape))
        .clickable(role = Role.Button, onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).then(if (primary) Modifier.background(Color.White.copy(alpha = .2f)) else Modifier.background(AuthGradient)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (primary) Color.White else AuthNavy)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = if (primary) Color.White.copy(alpha = .85f) else Color(0xFF4E6188))
            if (primary) Surface(shape = CircleShape, color = Color.White) { Text(copy("Fastest", "سب سے تیز"), color = Blue,
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) }
        }
        Text("›", color = if (primary) Color.White else AuthMuted, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun AuthHero(@DrawableRes icon: Int, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            for (size in listOf(160, 126, 98)) Box(Modifier.size(size.dp).border(1.dp, AuthMuted.copy(alpha = .17f), CircleShape))
            Box(Modifier.size(88.dp).clip(CircleShape).background(AuthGradient), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
        }
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { heading() })
        Text(subtitle, color = AuthMuted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun PhoneVerificationProgress(name: String?, stage: Int, onBack: () -> Unit, onCode: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AuthNavy).systemBarsPadding().verticalScroll(rememberScrollState())) {
        Box(Modifier.padding(24.dp)) { CustomerBackButton(onBack) }
        AuthHero(R.drawable.phone_outline, if (name.isNullOrBlank()) copy("Welcome back", "واپس خوش آمدید") else copy("Welcome back,\n$name", "واپس خوش آمدید،\n$name"),
            copy("Confirming this phone with Qist Locker.", "Qist Locker سے اس فون کی تصدیق ہو رہی ہے۔"))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Canvas).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val labels = listOf(copy("Qist Locker found on this phone", "اس فون پر Qist Locker موجود ہے"),
                copy("Phone matches your plan", "فون آپ کے منصوبے سے ملتا ہے"), copy("Signing you in", "آپ کو لاگ ان کر رہے ہیں"))
            labels.forEachIndexed { index, label ->
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (stage > index) Box(Modifier.size(28.dp).background(AuthCyan, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.detail_check), null, tint = AuthNavy, modifier = Modifier.size(18.dp))
                        } else if (stage == index) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        else Box(Modifier.size(28.dp).border(2.dp, Color(0xFFCAD4ED), CircleShape))
                        Text(label, style = MaterialTheme.typography.titleMedium, color = AuthNavy, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
            TextButton(onClick = onCode, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(copy("Use a code instead", "اس کی بجائے کوڈ استعمال کریں"))
            }
        }
    }
}
