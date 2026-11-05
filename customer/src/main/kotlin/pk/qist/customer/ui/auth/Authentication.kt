package pk.qist.customer.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.ui.*
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun Authentication(state: CustomerUiState, model: CustomerViewModel, onLanguage: () -> Unit) {
    var phone by rememberSaveable { mutableStateOf(state.recoveryPhone) }
    val page = state.authPage
    val back = { model.openAuth(when (page) { AuthPage.ResetPassword -> AuthPage.RequestPassword; AuthPage.RequestPassword -> AuthPage.Login; else -> AuthPage.Welcome }) }
    BackHandler(page != AuthPage.Welcome || state.busy) { if (!state.busy) back() }
    if (page == AuthPage.Welcome) { Welcome(onLanguage) { model.openAuth(AuthPage.Login) }; return }
    key(page) {
        var password by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        val title = when (page) { AuthPage.Login -> copy("Welcome back", "واپس خوش آمدید"); AuthPage.RequestPassword -> copy("Set or reset password", "پاس ورڈ بنائیں یا تبدیل کریں"); else -> copy("Check WhatsApp", "واٹس ایپ دیکھیں") }
        val subtitle = when (page) {
            AuthPage.Login -> copy("Sign in with the phone number your retailer registered.", "اپنے دکاندار کے رجسٹر کردہ نمبر سے سائن ان کریں۔")
            AuthPage.RequestPassword -> copy("We will send a verification code to your registered WhatsApp number. Use this to set your first password too.", "رجسٹرڈ واٹس ایپ نمبر پر تصدیقی کوڈ بھیجا جائے گا۔ پہلا پاس ورڈ بھی یہاں بنائیں۔")
            else -> copy("Enter the six-digit code for +92 ${state.recoveryPhone} and choose a new password.", "+92 ${state.recoveryPhone} کا چھ ہندسوں والا کوڈ اور نیا پاس ورڈ درج کریں۔")
        }
        val valid = when (page) {
            AuthPage.Login -> phone.length == 10 && phone.startsWith("3") && password.length in 8..128
            AuthPage.RequestPassword -> phone.length == 10 && phone.startsWith("3")
            else -> code.length == 6 && password.length in 8..128
        }
        val submit = {
            if (valid && !state.busy) {
                when (page) {
                    AuthPage.Login -> model.login(phone, password)
                    AuthPage.RequestPassword -> model.requestPassword(phone)
                    AuthPage.ResetPassword -> model.resetPassword(code, password)
                    AuthPage.Welcome -> Unit
                }
            }
        }
        Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(QistSpacing.AuthScreen)) {
            Column(verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                CustomerBackButton { if (!state.busy) back() }
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Caption(subtitle)
                if (page != AuthPage.ResetPassword) PhoneField(
                    phone, state.busy, page == AuthPage.RequestPassword, submit,
                ) { phone = normalizePhone(it) }
                if (page == AuthPage.ResetPassword) AuthField(
                    label = copy("WhatsApp code", "واٹس ایپ کوڈ"), value = code,
                    onValueChange = { code = it.mapNotNull { character -> character.digitToIntOrNull() }.joinToString("").take(6) },
                    enabled = !state.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                )
                if (page != AuthPage.RequestPassword) PasswordField(password, state.busy, page == AuthPage.ResetPassword, submit) { password = it }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (page == AuthPage.Login) TextButton(onClick = { model.openAuth(AuthPage.RequestPassword) }, enabled = !state.busy) { Text(copy("First time / Forgot password?", "پہلی بار / پاس ورڈ بھول گئے؟")) }
                if (page == AuthPage.ResetPassword) TextButton(onClick = { model.openAuth(AuthPage.RequestPassword) }, enabled = !state.busy) { Text(copy("Change number or request another code", "نمبر بدلیں یا نیا کوڈ حاصل کریں")) }
            }
            Spacer(Modifier.height(QistSpacing.Section))
            val label = when (page) { AuthPage.Login -> copy("Sign in", "سائن ان کریں"); AuthPage.RequestPassword -> copy("Send WhatsApp code", "واٹس ایپ کوڈ بھیجیں"); else -> copy("Save password", "پاس ورڈ محفوظ کریں") }
            Pill(label, submit, enabled = valid, loading = state.busy)
        }
    }
}

internal fun normalizePhone(input: String): String {
    val digits = input.mapNotNull { it.digitToIntOrNull() }.joinToString("")
    return digits.removePrefix("92").removePrefix("0").take(10)
}

@Composable
private fun PhoneField(phone: String, busy: Boolean, lastField: Boolean, onSubmit: () -> Unit, onChange: (String) -> Unit) {
    AuthField(
        label = copy("Phone number", "فون نمبر"), value = phone, onValueChange = onChange,
        enabled = !busy, placeholder = "3XX XXXXXXX", prefix = { Text("+92") },
        fieldDirection = LayoutDirection.Ltr,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = if (lastField) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
}

@Composable
private fun PasswordField(password: String, busy: Boolean, creatingPassword: Boolean, onSubmit: () -> Unit, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    AuthField(
        label = if (creatingPassword) copy("New password", "نیا پاس ورڈ") else copy("Password", "پاس ورڈ"),
        value = password, onValueChange = onChange, enabled = !busy,
        supportingText = if (creatingPassword) copy("8–128 characters", "۸ سے ۱۲۸ حروف") else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { TextButton(onClick = { visible = !visible }, enabled = !busy) { Text(if (visible) copy("Hide", "چھپائیں") else copy("Show", "دکھائیں")) } },
    )
}

@Composable
private fun AuthField(
    label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean,
    keyboardOptions: KeyboardOptions, keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholder: String? = null, supportingText: String? = null,
    fieldDirection: LayoutDirection = LocalLayoutDirection.current,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    prefix: @Composable (() -> Unit)? = null, trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        CompositionLocalProvider(LocalLayoutDirection provides fieldDirection) {
            OutlinedTextField(
                value = value, onValueChange = onValueChange, enabled = enabled, singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                shape = QistShapes.Input, textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = placeholder?.let { text -> { Text(text) } },
                supportingText = supportingText?.let { text -> { Text(text) } },
                prefix = prefix, trailingIcon = trailingIcon, visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}
@Composable
internal fun Welcome(onLanguage: () -> Unit, onContinue: () -> Unit) {
    CustomerSystemBars(lightIcons = true)
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
            CustomerBrand()
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
