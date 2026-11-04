package pk.qist.device.customer

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

internal enum class CustomerRoute { Welcome, Phone, Verify, Consent, Home }
internal enum class CustomerTab { Home, Payments, Plan, Profile }

private val LocalUrdu = compositionLocalOf { false }

@Composable
internal fun copy(en: String, ur: String): String = if (LocalUrdu.current) ur else en

@Composable
internal fun CustomerShell() {
    var route by rememberSaveable { mutableStateOf(CustomerRoute.Welcome) }
    var tab by rememberSaveable { mutableStateOf(CustomerTab.Home) }
    var urdu by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var paymentHelp by rememberSaveable { mutableStateOf(false) }
    val consent = LocalContext.current.getSharedPreferences("customer_privacy", 0)
    var consentPolicy by rememberSaveable { mutableStateOf(false) }
    val repository: CustomerRepository = DemoCustomerRepository

    fun back() {
        error = false
        route = if (route == CustomerRoute.Verify) CustomerRoute.Phone else CustomerRoute.Welcome
    }
    fun requestCode() {
        error = false
        code = ""
        deadline = SystemClock.elapsedRealtime() + 30_000
        route = CustomerRoute.Verify
    }

    BackHandler(route == CustomerRoute.Phone || route == CustomerRoute.Verify, onBack = ::back)
    BackHandler(route == CustomerRoute.Home && tab != CustomerTab.Home) { tab = CustomerTab.Home }
    BackHandler(route == CustomerRoute.Consent) {
        if (consentPolicy) consentPolicy = false else {
            phone = ""
            route = CustomerRoute.Welcome
        }
    }
    BackHandler(paymentHelp) { paymentHelp = false }
    CompositionLocalProvider(
        LocalUrdu provides urdu,
        LocalLayoutDirection provides if (urdu) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        when (route) {
            CustomerRoute.Welcome -> Welcome({ urdu = !urdu }, { route = CustomerRoute.Phone })
            CustomerRoute.Phone -> AuthScreen(
                title = copy("Enter your phone number", "اپنا فون نمبر درج کریں"),
                subtitle = copy("Use the number your retailer registered. Try any valid mobile number in this demo.", "ڈیمو میں کوئی بھی درست موبائل نمبر استعمال کریں۔"),
                action = copy("Send code", "کوڈ بھیجیں"),
                footer = copy("Not registered? Ask your retailer to add you.", "رجسٹرڈ نہیں؟ اپنے دکاندار سے رابطہ کریں۔"),
                onBack = ::back,
                onAction = { if (phone.matches(Regex("3[0-9]{9}"))) requestCode() else error = true },
            ) {
                PhoneField(phone, error) { phone = normalizePhone(it); error = false }
            }
            CustomerRoute.Verify -> AuthScreen(
                title = copy("Verify your number", "اپنے نمبر کی تصدیق کریں"),
                subtitle = copy("Enter the 6-digit code for +92 $phone. Demo code: 123456.", "+92 $phone کے لیے ۶ ہندسوں کا کوڈ درج کریں۔ ڈیمو کوڈ: 123456۔"),
                action = copy("Verify", "تصدیق کریں"),
                onBack = ::back,
                onAction = {
                    if (repository.verify(code)) {
                        tab = CustomerTab.Home
                        code = ""
                        route = if (consent.getBoolean("accepted", false)) CustomerRoute.Home else CustomerRoute.Consent
                    } else error = true
                },
            ) {
                CodeField(code) { code = it; error = false }
                if (error) Text(copy("Enter the demo code 123456.", "ڈیمو کوڈ 123456 درج کریں۔"), color = MaterialTheme.colorScheme.error)
                Resend(deadline, ::requestCode)
            }
            CustomerRoute.Consent -> if (consentPolicy) {
                ProfileInformation(repository.dashboard(), privacy = true) { consentPolicy = false }
            } else {
                PrivacyConsent(
                    onPolicy = { consentPolicy = true },
                    onAgree = {
                        consent.edit().putBoolean("accepted", true).apply()
                        route = CustomerRoute.Home
                    },
                    onDecline = {
                        phone = ""
                        route = CustomerRoute.Welcome
                    },
                )
            }
            CustomerRoute.Home -> if (paymentHelp) {
                HowToPay(repository.dashboard()) { paymentHelp = false }
            } else Dashboard(
                repository.dashboard(), tab, { tab = it }, { paymentHelp = true },
                phone = phone,
                onLanguage = { urdu = !urdu },
                onLogout = {
                    phone = ""
                    code = ""
                    error = false
                    deadline = 0L
                    paymentHelp = false
                    tab = CustomerTab.Home
                    route = CustomerRoute.Welcome
                },
            )
        }
    }
}

@Composable
private fun Welcome(onLanguage: () -> Unit, onContinue: () -> Unit) {
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
                Modifier.size(104.dp).background(Brush.linearGradient(listOf(Color(0xFF06B6D4), Blue)), CircleShape),
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
private fun AuthScreen(
    title: String,
    subtitle: String,
    action: String,
    onBack: () -> Unit,
    onAction: () -> Unit,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().imePadding().padding(24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val backLabel = copy("Back", "واپس")
            FilledIconButton(onClick = onBack, modifier = Modifier.size(48.dp).semantics { contentDescription = backLabel },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Navy),
            ) { Text(copy("‹", "›"), fontSize = 28.sp) }
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            content()
        }
        Pill(action, onAction)
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            Text(footer, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun normalizePhone(input: String): String {
    val digits = input.mapNotNull { it.digitToIntOrNull() }.joinToString("")
    return digits.removePrefix("92").removePrefix("0").take(10)
}

@Composable
private fun PhoneField(phone: String, error: Boolean, onChange: (String) -> Unit) {
    val label = copy("Phone number", "فون نمبر")
    Text(label, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFBFCBE4))) {
                Text("+92", modifier = Modifier.padding(horizontal = 20.dp, vertical = 17.dp), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = phone,
                onValueChange = onChange,
                modifier = Modifier.weight(1f).semantics { contentDescription = label },
                singleLine = true,
                placeholder = { Text("3XX XXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                isError = error,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
            )
        }
    }
    if (error) Text(copy("Enter a 10-digit mobile number starting with 3.", "۳ سے شروع ہونے والا ۱۰ ہندسوں کا نمبر درج کریں۔"), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun CodeField(code: String, onChange: (String) -> Unit) {
    val label = copy("Six-digit verification code", "چھ ہندسوں کا تصدیقی کوڈ")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = code,
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
                                Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(13.dp),
                                border = BorderStroke(if (index == code.length) 2.dp else 1.dp, if (index == code.length) Blue else Color(0xFFBFCBE4)),
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
private fun Resend(deadline: Long, onResend: () -> Unit) {
    var now by remember(deadline) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(deadline) {
        while (now < deadline) {
            delay(250)
            now = SystemClock.elapsedRealtime()
        }
    }
    val seconds = ((deadline - now + 999) / 1000).coerceAtLeast(0)
    Spacer(Modifier.height(12.dp))
    if (seconds == 0L) {
        TextButton(onClick = onResend) { Text(copy("Resend code", "کوڈ دوبارہ بھیجیں")) }
    } else {
        val time = "0:" + seconds.toString().padStart(2, '0')
        Text(copy("Resend code in $time", "کوڈ دوبارہ بھیجیں $time"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun Pill(label: String, onClick: () -> Unit, cyan: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (cyan) Cyan else Blue, contentColor = if (cyan) Navy else Color.White),
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

