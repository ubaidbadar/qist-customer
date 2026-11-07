package pk.qist.customer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = CardSurface, shape = QistShapes.Card) {
        Column(Modifier.padding(QistSpacing.Screen), content = content)
    }
}

@Composable
internal fun Pill(label: String, onClick: () -> Unit, cyan: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (cyan) QistSize.AuthButton else QistSize.Button),
        shape = QistShapes.Pill,
        colors = ButtonDefaults.buttonColors(containerColor = if (cyan) Cyan else Blue, contentColor = if (cyan) AccentText else CardSurface),
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@Composable
internal fun SecondaryAction(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = QistSize.Button), border = BorderStroke(QistSize.Border, InputBorder)) {
        Text(label, color = Navy)
    }
}

@Composable
internal fun ScreenBadge(icon: Int, color: Color, background: Color, size: Dp) {
    Box(Modifier.size(size).background(background, QistShapes.Pill), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(QistSize.IconMax), tint = color)
    }
}

@Composable
internal fun Badge(icon: Int, tint: Color, background: Color) {
    Box(Modifier.size(QistSize.CircleSmall).background(background, QistShapes.Pill), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(QistSize.Icon), tint = tint)
    }
}

@Composable
internal fun Caption(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
internal fun Chevron() {
    Icon(painterResource(pk.qist.customer.R.drawable.chevron_forward), null, Modifier.size(QistSize.IconMedium), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun DetailRow(
    title: String, subtitle: String, value: String, icon: Int,
    tint: Color = Blue, background: Color = BlueBackground,
    valueColor: Color = Navy, status: String? = null, onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).padding(vertical = QistSpacing.Item),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QistSpacing.IconGap),
    ) {
        Badge(icon, tint, background)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Caption(subtitle)
        }
        if (value.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End) {
                Text(value, color = valueColor, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                if (status != null) Text(status, color = tint, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (onClick != null) Chevron()
    }
}

@Composable
internal fun RetailerContactDialog(phone: String = "", whatsApp: Boolean = false, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val unavailable = copy("No app is available for this action.", "اس عمل کے لیے کوئی ایپ دستیاب نہیں۔")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy("Contact retailer", "دکاندار سے رابطہ")) },
        text = { Text(phone.ifBlank { copy("Your retailer has not provided a phone number.", "دکاندار نے فون نمبر فراہم نہیں کیا۔") }) },
        confirmButton = { TextButton(onClick = {
            if (phone.isNotBlank()) {
                val digits = phone.filter(Char::isDigit).let { if (it.startsWith("0")) "92" + it.drop(1) else it }
                val uri = if (whatsApp) "https://wa.me/$digits" else "tel:$phone"
                val action = if (whatsApp) android.content.Intent.ACTION_VIEW else android.content.Intent.ACTION_DIAL
                try { context.startActivity(android.content.Intent(action, android.net.Uri.parse(uri))) }
                catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context, unavailable, android.widget.Toast.LENGTH_SHORT).show() }
            }
            onDismiss()
        }) { Text(if (phone.isBlank()) copy("Close", "بند کریں") else if (whatsApp) copy("WhatsApp", "واٹس ایپ") else copy("Call", "کال کریں")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(copy("Cancel", "منسوخ")) } },
    )
}

@Composable
internal fun money(amount: Long): String {
    val formatted = pk.qist.customer.data.formatAmount(amount)
    return copy("Rs. $formatted", "روپے $formatted")
}

@Composable
internal fun CustomerBackButton(onBack: () -> Unit) {
    FilledIconButton(
        onClick = onBack,
        modifier = Modifier.size(QistSize.TouchTarget),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardSurface, contentColor = Navy),
    ) {
        Icon(painterResource(pk.qist.customer.R.drawable.notice_back), contentDescription = copy("Back", "واپس"), modifier = Modifier.size(QistSize.IconMax))
    }
}
