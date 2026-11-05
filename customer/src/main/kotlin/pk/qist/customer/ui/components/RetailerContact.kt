package pk.qist.customer.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.QistSpacing

@Composable
internal fun RetailerContactActions(phone: String) {
    val context = LocalContext.current
    val unavailable = copy("No app is available to open this action.", "یہ عمل کھولنے کے لیے ایپ دستیاب نہیں۔")
    var missing by remember { mutableStateOf(false) }
    fun open(whatsapp: Boolean) {
        val digits = phone.filter(Char::isDigit)
        if (digits.isEmpty()) { missing = true; return }
        val international = if (digits.startsWith("0")) "92${digits.drop(1)}" else digits
        val intent = if (whatsapp) Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$international"))
            else Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        try { context.startActivity(intent) }
        catch (_: ActivityNotFoundException) { Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show() }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        Button(onClick = { open(false) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(copy("Call", "کال کریں")) }
        OutlinedButton(onClick = { open(true) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(copy("WhatsApp", "واٹس ایپ")) }
    }
    if (missing) RetailerContactDialog { missing = false }
}
