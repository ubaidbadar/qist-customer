package pk.qist.customer.ui.payments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.data.PaymentMethodSample
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentMethodsSheet(data: DashboardSample, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = QistShapes.Sheet, containerColor = Color.White, scrimColor = Navy.copy(alpha = 0.72f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = InputBorder, width = 40.dp) },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = QistSpacing.Screen).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(copy("Payment methods", "ادائیگی کے طریقے"), style = MaterialTheme.typography.titleLarge)
            Text(copy("Pay ${data.retailer} directly using any of these.", "${data.retailer} کو ان طریقوں سے براہ راست ادائیگی کریں۔"),
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PaymentMethods(data, compact = true)
            PaymentNotice()
            Pill(copy("Close", "بند کریں"), onDismiss)
        }
    }
}

@Composable
internal fun PaymentMethods(data: DashboardSample, compact: Boolean = false) {
    if (data.paymentMethods.isEmpty()) Text(copy("Ask your retailer for payment details.", "ادائیگی کی تفصیل دکاندار سے پوچھیں۔"))
    data.paymentMethods.forEach { method ->
        PaymentMethodCard(method, data.retailerAddress, compact)
    }
}

@Composable
private fun PaymentMethodCard(method: PaymentMethodSample, address: String, compact: Boolean) {
    val context = LocalContext.current
    val copied = copy("Demo account details copied", "نمونہ اکاؤنٹ کی تفصیل کاپی ہو گئی")
    Surface(color = if (compact) Canvas else Color.White, shape = if (compact) QistShapes.Option else QistShapes.Card) {
        Row(Modifier.fillMaxWidth().padding(horizontal = if (compact) 14.dp else 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)) {
            ScreenBadge(if (method.cash) R.drawable.payment_shop else if (method.bank.isNotEmpty()) R.drawable.payment_bank else R.drawable.phone_outline,
                Blue, BlueBackground, if (compact) 40 else 44)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (method.cash) copy("Cash at shop", "دکان پر نقد") else method.title, style = MaterialTheme.typography.titleSmall)
                Text(if (method.cash) address else listOf(method.bank, method.accountTitle).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!method.cash) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(method.accountNumber, style = MaterialTheme.typography.titleSmall)
                }
            }
            if (!method.cash) OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(method.title, method.accountNumber))
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                }, shape = CircleShape, contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 12.dp),
                modifier = Modifier.heightIn(min = 48.dp), border = BorderStroke(1.dp, InputBorder),
            ) {
                Icon(painterResource(R.drawable.payment_copy), null, Modifier.size(16.dp), tint = Navy)
                Spacer(Modifier.width(6.dp))
                Text(copy("Copy", "کاپی"), color = Navy)
            }
        }
    }
}

@Composable
internal fun PaymentNotice() {
    Surface(color = BlueBackground, shape = QistShapes.Option) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.payment_info), null, Modifier.size(18.dp), tint = Blue)
            Text(copy("Qist does not process payments. Pay your retailer directly and keep your receipt.", "قسط ادائیگی وصول نہیں کرتا۔ اپنے دکاندار کو براہ راست ادائیگی کریں اور رسید محفوظ رکھیں۔"),
                style = MaterialTheme.typography.bodyMedium, color = Navy)
        }
    }
}
