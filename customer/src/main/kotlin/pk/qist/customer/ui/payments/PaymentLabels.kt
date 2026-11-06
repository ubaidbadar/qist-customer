package pk.qist.customer.ui.payments

import androidx.compose.runtime.Composable
import pk.qist.customer.ui.localization.copy

@Composable
internal fun paymentMethodLabel(method: String): String = when (method) {
    "cash" -> copy("Cash at shop", "دکان پر نقد")
    "easypaisa" -> copy("Easypaisa", "ایزی پیسہ")
    "jazzcash" -> copy("JazzCash", "جاز کیش")
    "bank" -> copy("Bank transfer", "بینک ٹرانسفر")
    else -> method
}
