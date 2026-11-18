package pk.qist.customer.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import pk.qist.customer.R
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
    LaunchedEffect(model) { model.checkLockerAvailability() }
    val footer = if (state.lockerAvailable == true) {
        copy("Qist Lock found on this phone. Not registered? Ask your retailer to add you.",
            "Qist Lock اس فون پر موجود ہے۔ رجسٹرڈ نہیں؟ اپنے دکاندار سے رابطہ کریں۔")
    } else {
        copy("Not registered? Ask your retailer to add you.", "رجسٹرڈ نہیں؟ اپنے دکاندار سے رابطہ کریں۔")
    }
    AuthScreen(
        copy("Enter your phone number", "اپنا فون نمبر درج کریں"),
        copy("Use the number your retailer registered.", "اپنے دکاندار کا رجسٹرڈ نمبر استعمال کریں۔"),
        copy("Continue", "جاری رکھیں"),
        model::back,
        { model.start(phone) },
        footer = footer,
        bottomContent = {
            if (state.lockerAvailable == true) {
                Spacer(Modifier.height(12.dp))
                AuthDivider()
                Spacer(Modifier.height(12.dp))
                AuthSecondaryAction(
                    label = copy("Sign in with Qist Lock app", "Qist Lock ایپ سے لاگ ان کریں"),
                    icon = R.drawable.phone_outline,
                    onClick = { model.askLockerSignin(phone) },
                    enabled = !state.busy,
                )
            }
        },
        busy = state.busy,
        enabled = phone.matches(customerPhonePattern) && !state.phoneNotRegistered,
    ) {
        PhoneField(phone, state.phoneNotRegistered) {
            if (!state.busy) { phone = normalizePhone(it); model.editPhone() }
        }
        PhoneNumberHint(phone.isEmpty(), !state.busy, showAction = false) {
            phone = it
            model.editPhone()
        }
        AuthError(state.message)
    }
}

@Composable
internal fun PasswordSigninScreen(state: CustomerState, model: CustomerViewModel, accepted: Boolean) {
    val creating = state.route == CustomerRoute.SetPassword
    var password by remember(creating) { mutableStateOf("") }
    LaunchedEffect(model) { model.checkLockerAvailability() }
    AuthScreen(
        if (creating) copy("Create your password", "اپنا پاس ورڈ بنائیں") else copy("Enter your password", "اپنا پاس ورڈ درج کریں"),
        if (creating) copy("Your number is verified. Choose a password with 8–128 characters.", "نمبر کی تصدیق ہو گئی۔ ۸ تا ۱۲۸ حروف کا پاس ورڈ بنائیں۔")
        else copy("Signing in as +92 ${state.phone}.", "+92 ${state.phone} کے طور پر لاگ ان ہو رہے ہیں۔"),
        if (creating) copy("Save and continue", "محفوظ کریں اور جاری رکھیں") else copy("Sign in", "لاگ ان کریں"),
        model::back,
        { if (creating) model.savePassword(password, accepted) else model.login(password, accepted) },
        bottomContent = {
            if (!creating) {
                Spacer(Modifier.height(12.dp))
                AuthDivider()
                Spacer(Modifier.height(12.dp))
                if (state.lockerAvailable == true) {
                    AuthSecondaryAction(
                        label = copy("Sign in with Qist Lock app", "Qist Lock ایپ سے لاگ ان کریں"),
                        icon = R.drawable.phone_outline,
                        onClick = model::askLockerSignin,
                        enabled = !state.busy,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                AuthSecondaryAction(
                    label = copy("Get code in Qist Lock app", "Qist Lock ایپ میں کوڈ حاصل کریں"),
                    icon = R.drawable.detail_bell,
                    onClick = { model.requestApproval(state.phone) },
                    enabled = !state.busy,
                )
            }
        },
        busy = state.busy,
        enabled = password.length in 8..128,
    ) {
        PasswordField(password, creating, enabled = !state.busy) {
            if (!state.busy) password = it.take(128)
        }
        if (!creating) {
            TextButton(
                onClick = { model.requestApproval(state.phone) },
                modifier = Modifier.align(Alignment.End),
                enabled = !state.busy,
            ) {
                Text(copy("Forgot password?", "پاس ورڈ بھول گئے؟"))
            }
        } else {
            TextButton(onClick = { model.requestApproval(state.phone) }, enabled = !state.busy) {
                Text(copy("Verify again", "دوبارہ تصدیق کریں"))
            }
        }
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
    VerificationScreen(
        state.phone, code, state.busy, clipboardError ?: state.message, state.codeRejected,
        state.deadline, state.verifyBlockedUntil, model::back, { model.verify(code) },
        { code = it; clipboardError = null; model.editCode() },
        { code = ""; model.requestCode() },
        extra = {
            AuthSecondaryAction(
                label = copy("Paste code from Qist Lock", "Qist Lock سے کوڈ پیسٹ کریں"),
                icon = R.drawable.detail_bell,
                onClick = {
                    val manager = context.getSystemService(android.content.ClipboardManager::class.java)
                    val pasted = try { manager.primaryClip?.getItemAt(0)?.text?.toString()?.trim() } catch (_: SecurityException) { null }
                    if (pasted?.matches(customerVerificationCodePattern) == true) {
                        code = pasted
                        clipboardError = null
                        model.editCode()
                    } else {
                        clipboardError = "Copy the six-digit code from Qist Lock first."
                    }
                },
                enabled = !state.busy,
            )
        },
    )
}
