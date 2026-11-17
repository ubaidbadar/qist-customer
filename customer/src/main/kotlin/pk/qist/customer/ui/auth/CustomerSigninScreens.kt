package pk.qist.customer.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import pk.qist.customer.platform.PhoneNumberHint
import pk.qist.customer.ui.CustomerRoute
import pk.qist.customer.ui.CustomerState
import pk.qist.customer.ui.CustomerViewModel
import pk.qist.customer.ui.customerPhonePattern
import pk.qist.customer.ui.customerVerificationCodePattern
import pk.qist.customer.ui.localization.copy

/** Text entry invalidates only this screen; secrets are never saved with instance state. */
@Composable
internal fun PhoneSigninScreen(state: CustomerState, model: CustomerViewModel) {
    var phone by rememberSaveable { mutableStateOf(state.phone) }
    AuthScreen(
        copy("Enter your phone number", "اپنا فون نمبر درج کریں"),
        copy("Use the number registered with your retailer.", "اپنے دکاندار کے پاس رجسٹرڈ نمبر استعمال کریں۔"),
        copy("Continue", "جاری رکھیں"), model::back, { model.start(phone) },
        footer = copy("Not registered? Contact your retailer.", "رجسٹرڈ نہیں؟ دکاندار سے رابطہ کریں۔"), busy = state.busy,
        enabled = phone.matches(customerPhonePattern) && !state.phoneNotRegistered,
    ) {
        PhoneField(phone, state.phoneNotRegistered) { if (!state.busy) { phone = normalizePhone(it); model.editPhone() } }
        PhoneNumberHint(phone.isEmpty(), !state.busy) { phone = it; model.editPhone() }


        AuthError(state.message)
    }
}

@Composable
internal fun PasswordSigninScreen(state: CustomerState, model: CustomerViewModel, accepted: Boolean) {
    val creating = state.route == CustomerRoute.SetPassword
    var password by remember(creating) { mutableStateOf("") }
    AuthScreen(
        if (creating) copy("Create your password", "اپنا پاس ورڈ بنائیں") else copy("Welcome back", "واپس خوش آمدید"),
        if (creating) copy("Your number is verified. Choose a password with 8–128 characters.", "نمبر کی تصدیق ہو گئی۔ ۸ تا ۱۲۸ حروف کا پاس ورڈ بنائیں۔") else copy("Enter your password for +92 ${state.phone}.", "+92 ${state.phone} کا پاس ورڈ درج کریں۔"),
        if (creating) copy("Save and continue", "محفوظ کریں اور جاری رکھیں") else copy("Sign in", "لاگ ان کریں"),
        model::back, { if (creating) model.savePassword(password, accepted) else model.login(password, accepted) },
        busy = state.busy, enabled = password.length in 8..128,
    ) {
        PasswordField(password, creating) { if (!state.busy) password = it.take(128) }
        TextButton(onClick = { model.requestApproval(state.phone) }, enabled = !state.busy) {
            Text(if (creating) copy("Verify again", "دوبارہ تصدیق کریں") else copy("Get a code from your financed phone", "قسط والے فون سے کوڈ لیں"))
        }
        if (!creating) TextButton(onClick = model::askLockerSignin, enabled = !state.busy) { Text(copy("Sign in with Locker on this phone", "اسی فون کے لاکر سے لاگ ان کریں")) }
        AuthError(state.message)
    }
}

@Composable
internal fun CodeSigninScreen(state: CustomerState, model: CustomerViewModel) {
    var code by remember { mutableStateOf("") }
    var clipboardError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(state.verifyBlockedUntil) {
        if (state.verifyBlockedUntil > android.os.SystemClock.elapsedRealtime()) code = ""
    }
    VerificationScreen(state.phone, code, state.busy, clipboardError ?: state.message, state.codeRejected,
        state.deadline, state.verifyBlockedUntil, model::back, { model.verify(code) },
        { code = it; clipboardError = null; model.editCode() }, { code = ""; model.requestCode() }, extra = {
            OutlinedButton(onClick = {
                val manager = context.getSystemService(android.content.ClipboardManager::class.java)
                val pasted = try { manager.primaryClip?.getItemAt(0)?.text?.toString()?.trim() } catch (_: SecurityException) { null }
                if (pasted?.matches(customerVerificationCodePattern) == true) { code = pasted; clipboardError = null; model.editCode() }
                else clipboardError = "Copy the six-digit code from Qist Locker first."
            }, enabled = !state.busy) { Text(copy("Paste code", "کوڈ پیسٹ کریں")) }
            TextButton(onClick = model::askLockerSignin, enabled = !state.busy) {
                Text(copy("Sign in with Qist Locker on this phone", "اسی فون پر Qist Locker سے لاگ ان کریں"))
            }
            if (state.passwordAvailable) {
                TextButton(onClick = { model.usePassword(state.phone) }, enabled = !state.busy) {
                    Text(copy("Use password instead", "پاس ورڈ استعمال کریں"))
                }
            }
        })
}
