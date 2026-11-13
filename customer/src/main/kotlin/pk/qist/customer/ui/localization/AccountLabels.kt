package pk.qist.customer.ui.localization

import pk.qist.customer.data.PaymentSample

/** Shared by Home, payment history and exported receipts. Unknown server labels stay readable. */
internal fun paymentLabel(payment: PaymentSample, urdu: Boolean): String {
    if (!urdu) return payment.label
    return when {
        payment.kind == "down_payment" -> "پیشگی ادائیگی"
        payment.allocations.isEmpty() -> "ادائیگی"
        else -> "قسط ${payment.allocations.joinToString("، ")}"
    }
}

internal fun paymentMethodLabel(method: String, urdu: Boolean): String = when (method) {
    "cash" -> if (urdu) "نقد" else "Cash"
    "bank" -> if (urdu) "بینک ٹرانسفر" else "Bank transfer"
    "easypaisa" -> if (urdu) "ایزی پیسہ" else "Easypaisa"
    "jazzcash" -> if (urdu) "جاز کیش" else "JazzCash"
    "other" -> if (urdu) "دیگر" else "Other"
    else -> method
}

internal fun frequencyLabel(frequency: String, urdu: Boolean): String = when (frequency) {
    "weekly" -> if (urdu) "ہفتہ وار" else "Weekly"
    "biweekly" -> if (urdu) "ہر دو ہفتے" else "Every two weeks"
    "monthly" -> if (urdu) "ماہانہ" else "Monthly"
    else -> frequency
}
