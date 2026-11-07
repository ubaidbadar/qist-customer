package pk.qist.customer.ui.auth

import android.os.SystemClock
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import pk.qist.customer.R
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun Welcome(onLanguage: () -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Navy).systemBarsPadding().padding(horizontal = QistSpacing.AuthScreen, vertical = QistSpacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(onClick = onLanguage, modifier = Modifier.align(Alignment.End).heightIn(min = QistSize.TouchTarget)) {
            Text(copy("اردو", "English"), color = CardSurface)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(QistSize.Logo).background(Brush.linearGradient(listOf(BrandGradientStart, Blue)), QistShapes.Pill),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(QistSpacing.Screen))
            Text("Qist", color = CardSurface, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(QistSpacing.AuthScreen))
            Text(copy("Your installments, always clear.", "آپ کی اقساط، ہمیشہ واضح۔"), color = CardSurface, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(QistSpacing.Section))
            Text(copy("See your plan, due dates and payments in one place.", "اپنا منصوبہ، آخری تاریخیں اور ادائیگیاں ایک جگہ دیکھیں۔"), color = Muted, textAlign = TextAlign.Center)
        }
        Pill(copy("Get started", "شروع کریں"), onContinue, true)
        Spacer(Modifier.height(QistSpacing.Item))
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
            val backLabel = copy("Back", "واپس")
            FilledIconButton(onClick = onBack, enabled = !busy, modifier = Modifier.size(QistSize.TouchTarget).semantics { contentDescription = backLabel },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardSurface, contentColor = Navy),
            ) { Icon(painterResource(R.drawable.notice_back), contentDescription = null, modifier = Modifier.size(QistSize.IconMax)) }
            Spacer(Modifier.height(QistSpacing.Heading))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(QistSpacing.IconGap))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(QistSpacing.AuthScreen))
            content()
        }
        Button(onClick = onAction, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = QistSize.AuthButton), shape = QistShapes.Pill,
            colors = ButtonDefaults.buttonColors(disabledContainerColor = DisabledBackground, disabledContentColor = DisabledText),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(QistSize.IconMedium), strokeWidth = QistSize.Stroke, color = CardSurface)
                Spacer(Modifier.width(QistSpacing.Small))
            }
            Text(if (busy) copy("Please wait…", "انتظار کریں…") else action)
        }
        if (footer != null) {
            Spacer(Modifier.height(QistSpacing.Item))
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
    Spacer(Modifier.height(QistSpacing.Small))
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(QistSpacing.IconGap), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = QistShapes.Input, border = BorderStroke(QistSize.Border, InputBorder)) {
                Text("+92", modifier = Modifier.padding(horizontal = QistSpacing.Screen, vertical = QistSpacing.Section), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = phone,
                onValueChange = onChange,
                modifier = Modifier.weight(1f).heightIn(min = QistSize.Field).semantics { contentDescription = label },
                singleLine = true,
                placeholder = { Text("3XX XXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = QistShapes.Input,
                isError = error,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CardSurface, focusedContainerColor = CardSurface,
                    unfocusedBorderColor = InputOutline, focusedBorderColor = Blue, errorBorderColor = Danger,
                    unfocusedPlaceholderColor = PlaceholderText, focusedPlaceholderColor = PlaceholderText,
                ),
            )
        }
    }
    if (error) Text(copy("Enter a 10-digit mobile number starting with 3.", "۳ سے شروع ہونے والا ۱۰ ہندسوں کا نمبر درج کریں۔"), color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun CodeField(code: String, onChange: (String) -> Unit) {
    val label = copy("Six-digit verification code", "چھ ہندسوں کا تصدیقی کوڈ")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = code,
            onValueChange = { input -> onChange(input.mapNotNull { it.digitToIntOrNull() }.take(6).joinToString("")) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Clear),
            cursorBrush = SolidColor(Clear),
            decorationBox = { field ->
                Box {
                    Row(
                        Modifier.widthIn(max = QistSize.OtpWidth * 6 + QistSpacing.Compact * 5).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(QistSpacing.Compact),
                    ) {
                        repeat(6) { index ->
                            Surface(
                                Modifier.weight(1f).height(QistSize.OtpHeight),
                                shape = QistShapes.CodeCell,
                                border = BorderStroke(if (index == code.length) QistSize.FocusBorder else QistSize.Border, if (index == code.length) Blue else InputOutline),
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
    var now by remember(deadline) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(deadline) {
        while (now < deadline) {
            delay(250)
            now = SystemClock.elapsedRealtime()
        }
    }
    val seconds = ((deadline - now + 999) / 1000).coerceAtLeast(0)
    Spacer(Modifier.height(QistSpacing.Item))
    if (seconds == 0L) {
        TextButton(onClick = onResend, enabled = enabled) { Text(copy("Resend code", "کوڈ دوبارہ بھیجیں")) }
    } else {
        val time = "0:" + seconds.toString().padStart(2, '0')
        Text(copy("Resend code in $time", "کوڈ دوبارہ بھیجیں $time"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Visibility stays local; passwords never enter saved instance state. */
@Composable
internal fun PasswordField(password: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password, onValueChange = onChange, modifier = Modifier.fillMaxWidth().heightIn(min = QistSize.Field), singleLine = true,
        label = { Text(copy("Password", "پاس ورڈ")) }, shape = QistShapes.Input,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(painterResource(if (visible) R.drawable.password_eye_off else R.drawable.password_eye),
                    modifier = Modifier.size(QistSize.IconMax),
                    contentDescription = if (visible) copy("Hide password", "پاس ورڈ چھپائیں") else copy("Show password", "پاس ورڈ دکھائیں"))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = CardSurface, focusedContainerColor = CardSurface,
            unfocusedBorderColor = InputOutline, focusedBorderColor = Blue, errorBorderColor = Danger,
            unfocusedPlaceholderColor = PlaceholderText, focusedPlaceholderColor = PlaceholderText,
        ),
    )
}
