package pk.qist.customer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import java.text.NumberFormat
import java.util.Locale
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.Cyan
import pk.qist.customer.ui.theme.InputBorder
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = Color.White, shape = QistShapes.Card) {
        Column(Modifier.padding(QistSpacing.Screen), content = content)
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

@Composable
internal fun SecondaryAction(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), border = BorderStroke(1.dp, InputBorder)) {
        Text(label, color = Navy)
    }
}

@Composable
internal fun ScreenBadge(icon: Int, color: Color, background: Color, size: Int) {
    Box(Modifier.size(size.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = color)
    }
}

@Composable
internal fun Badge(icon: Int, tint: Color, background: Color) {
    Box(Modifier.size(36.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(19.dp), tint = tint)
    }
}

@Composable
internal fun Caption(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
internal fun Chevron() {
    Text(copy("›", "‹"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun DetailRow(
    title: String, subtitle: String, value: String, icon: Int,
    tint: Color = Blue, background: Color = Blue.copy(alpha = 0.08f),
    valueColor: Color = Navy, status: String? = null, onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
internal fun RetailerContactDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy("Contact retailer", "دکاندار سے رابطہ")) },
        text = { Text(copy("This demo has no retailer phone number.", "ڈیمو میں دکاندار کا نمبر موجود نہیں۔")) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(copy("Close", "بند کریں")) } },
    )
}

@Composable
internal fun money(amount: Int): String {
    val formatted = NumberFormat.getIntegerInstance(Locale.US).format(amount)
    return copy("Rs. $formatted", "روپے $formatted")
}

@Composable
internal fun CustomerBackButton(onBack: () -> Unit) {
    FilledIconButton(
        onClick = onBack,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Navy),
    ) {
        Icon(painterResource(pk.qist.customer.R.drawable.notice_back), contentDescription = copy("Back", "واپس"))
    }
}
