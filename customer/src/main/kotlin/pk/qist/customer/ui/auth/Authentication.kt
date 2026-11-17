package pk.qist.customer.ui.auth

import android.os.SystemClock
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import pk.qist.customer.R
import pk.qist.customer.ui.components.CustomerBackButton
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.BrandGradientStart
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.InputBorder
import pk.qist.customer.ui.theme.Muted
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun Welcome(onLanguage: () -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Navy).systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(onClick = onLanguage, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
            Text(copy("اردو", "English"), color = Color.White)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(104.dp).background(Brush.linearGradient(listOf(BrandGradientStart, Blue)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(20.dp))
            Text("Qist", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            Text(copy("Your installments, always clear.", "آپ کی اقساط، ہمیشہ واضح۔"), color = Color.White, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(copy("See your plan, due dates and payments in one place.", "اپنا منصوبہ، آخری تاریخیں اور ادائیگیاں ایک جگہ دیکھیں۔"), color = Muted, textAlign = TextAlign.Center)
        }
        Pill(copy("Get started", "شروع کریں"), onContinue, true)
        Spacer(Modifier.height(12.dp))
        Text(copy("New here? Your retailer adds you to Qist.", "نئے ہیں؟ آپ کا دکاندار آپ کو قسط میں شامل کرتا ہے۔"), color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun AuthScreen(
    title: String,
    subtitle: String,
    action: String,
    onBack: () -> Unit,
    onAction: () -> Unit,
    footer: String? = null,
    busy: Boolean = false,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().imePadding().padding(QistSpacing.AuthScreen)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CustomerBackButton(onBack, enabled = !busy)
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            content()
        }
        Pill(if (busy) copy("Please wait…", "انتظار کریں…") else action, onAction, enabled = enabled, busy = busy)
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            Text(footer, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun normalizePhone(input: String): String {
    val digits = input.mapNotNull { it.digitToIntOrNull() }.joinToString("")
    return digits.removePrefix("92").removePrefix("0").take(10)
}

@Composable
internal fun PhoneField(phone: String, error: Boolean, onChange: (String) -> Unit) {
    val label = copy("Phone number", "فون نمبر")
    Text(label, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = QistShapes.Input, border = BorderStroke(1.dp, InputBorder)) {
                Text("+92", modifier = Modifier.padding(horizontal = 20.dp, vertical = 17.dp), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = phone,
                onValueChange = onChange,
                modifier = Modifier.weight(1f).semantics { contentDescription = label; contentType = ContentType.PhoneNumber + ContentType.Username },
                singleLine = true,
                placeholder = { Text("3XX XXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = QistShapes.Input,
                isError = error,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
            )
        }
    }
}

@Composable
internal fun CodeField(code: String, error: Boolean = false, enabled: Boolean = true, onChange: (String) -> Unit) {
    val label = copy("Six-digit verification code", "چھ ہندسوں کا تصدیقی کوڈ")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = code, enabled = enabled,
            onValueChange = { input -> onChange(input.mapNotNull { it.digitToIntOrNull() }.take(6).joinToString("")) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { field ->
                Box {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(6) { index ->
                            Surface(
                                Modifier.weight(1f).heightIn(min = 56.dp),
                                shape = QistShapes.CodeCell,
                                color = if (!enabled) pk.qist.customer.ui.theme.Divider else if (error) pk.qist.customer.ui.theme.DestructiveBackground.copy(alpha = 0.4f) else Color.White,
                                border = BorderStroke(if (error || (enabled && index == code.length)) 2.dp else 1.dp,
                                    if (error) pk.qist.customer.ui.theme.Destructive else if (enabled && index == code.length) Blue else InputBorder),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(code.getOrNull(index)?.toString().orEmpty(), style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                    Box(Modifier.matchParentSize()) { field() }
                }
            },
        )
    }
}

@Composable
internal fun Resend(deadline: Long, onResend: () -> Unit, enabled: Boolean = true) {
    val seconds by rememberRemainingSeconds(deadline)
    Spacer(Modifier.height(12.dp))
    if (seconds == 0L) {
        TextButton(onClick = onResend, enabled = enabled) { Text(copy("Resend code", "کوڈ دوبارہ بھیجیں")) }
    } else {
        val time = "${seconds / 60}:" + (seconds % 60).toString().padStart(2, '0')
        Text(copy("Resend code in $time", "کوڈ دوبارہ بھیجیں $time"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Visibility stays local; passwords never enter saved instance state. */
@Composable
internal fun PasswordField(password: String, creating: Boolean = false, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password, onValueChange = onChange, modifier = Modifier.fillMaxWidth().semantics { contentType = if (creating) ContentType.NewPassword else ContentType.Password }, singleLine = true,
        label = { Text(copy("Password", "پاس ورڈ")) }, shape = QistShapes.Input,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(painterResource(if (visible) R.drawable.password_eye_off else R.drawable.password_eye),
                    contentDescription = if (visible) copy("Hide password", "پاس ورڈ چھپائیں") else copy("Show password", "پاس ورڈ دکھائیں"))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
    )
}

/** The timer writes only this state; readers choose whether they need seconds or just expiry. */
@Composable
private fun rememberRemainingSeconds(deadline: Long, clock: () -> Long = { SystemClock.elapsedRealtime() }): State<Long> {
    fun remaining() = ((deadline - clock() + 999) / 1000).coerceAtLeast(0)
    val seconds = remember(deadline, clock) { mutableLongStateOf(remaining()) }
    LaunchedEffect(deadline, clock) {
        while (seconds.longValue > 0) { delay(1000); seconds.longValue = remaining() }
    }
    return seconds
}

@Composable
internal fun VerificationScreen(phone: String, code: String, busy: Boolean, message: String?, codeRejected: Boolean,
    resendAt: Long, blockedUntil: Long, onBack: () -> Unit, onVerify: () -> Unit, onCode: (String) -> Unit, onResend: () -> Unit, clock: () -> Long = { SystemClock.elapsedRealtime() }) {
    val remaining = rememberRemainingSeconds(blockedUntil, clock)
    val blocked by remember(remaining) { derivedStateOf { remaining.value > 0 } }
    AuthScreen(copy("Verify your number", "اپنے نمبر کی تصدیق کریں"),
        copy("Enter the 6-digit code sent to +92 $phone on WhatsApp.", "+92 $phone کے واٹس ایپ پر بھیجا گیا ۶ ہندسوں کا کوڈ درج کریں۔"),
        copy("Verify", "تصدیق کریں"), onBack, onVerify, busy = busy, enabled = code.length == 6 && !blocked) {
        CodeField(code, error = codeRejected, enabled = !busy && !blocked, onChange = onCode)
        if (blocked) VerificationWait(remaining) else {
            AuthError(message)
            Resend(resendAt, onResend, enabled = !busy)
        }
    }
}

@Composable
internal fun AuthError(message: String?) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun VerificationWait(remaining: State<Long>) {
    val seconds = remaining.value
    val time = "${seconds / 60}:" + (seconds % 60).toString().padStart(2, '0')
    Surface(color = pk.qist.customer.ui.theme.DestructiveBackground, shape = QistShapes.Input, modifier = Modifier.padding(top = 16.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.detail_lock), null, Modifier.size(20.dp), tint = pk.qist.customer.ui.theme.Destructive)
            Text(copy("Too many attempts. Try again in $time.", "بہت زیادہ کوششیں۔ $time بعد دوبارہ کوشش کریں۔"), color = pk.qist.customer.ui.theme.Destructive)
        }
    }
}
