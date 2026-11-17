package pk.qist.customer.ui.notifications

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.components.CustomerBottomSheet
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.AccountEmptyState
import pk.qist.customer.ui.components.CustomerBackButton
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.BlueBackground
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.Destructive
import pk.qist.customer.ui.theme.DestructiveBackground
import pk.qist.customer.ui.theme.DueAmber
import pk.qist.customer.ui.theme.DueBackground
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.PaidBackground
import pk.qist.customer.ui.theme.PaidGreen
import pk.qist.customer.ui.theme.QistShapes
import pk.qist.customer.ui.theme.QistSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationPrompt(onAllow: () -> Unit, onDismiss: () -> Unit) {
    CustomerBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(QistSpacing.Section),
        ) {
            NoticeIcon(R.drawable.detail_bell, Blue, BlueBackground, large = true)
            Text(copy("Never miss a due date", "قسط کی تاریخ یاد رکھیں"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                copy("Get due-date reminders, payment confirmations and phone-status updates. You can change this in Profile.", "قسط کی یاد دہانیاں، ادائیگی کی تصدیق اور فون کی حالت کی اطلاعات پائیں۔ اسے پروفائل میں بدل سکتے ہیں۔"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Pill(copy("Allow notifications", "اطلاعات کی اجازت دیں"), onAllow)
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(copy("Not now", "ابھی نہیں"))
            }
        }
    }
}

@Composable
internal fun NotificationInbox(notices: List<pk.qist.customer.data.CustomerNotice>, error: String?, busy: Boolean,
    onBack: () -> Unit, onReadAll: () -> Unit, onOpen: (pk.qist.customer.data.CustomerNotice) -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(QistSpacing.Screen)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CustomerBackButton(onBack)
            Text(copy("Notifications", "اطلاعات"), style = MaterialTheme.typography.headlineMedium)
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        if (notices.isEmpty()) BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            AccountEmptyState(R.drawable.detail_bell, copy("No notifications yet", "ابھی کوئی اطلاع نہیں"),
                copy("No notifications are available yet. Check Home and Payments for current account information.", "ابھی کوئی اطلاع دستیاب نہیں۔ موجودہ معلومات کے لیے ہوم اور ادائیگیاں دیکھیں۔"),
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight))
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "read-all", contentType = "action") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onReadAll, enabled = !busy && notices.any { !it.read }) { Text(copy("Mark all read", "سب پڑھ لیے")) }
                }
            }
            items(notices, key = { it.id }, contentType = { "notification" }) { notice ->
                NoticeCard(notice.title, notice.body, notice.date,
                    if (notice.kind.startsWith("payment")) R.drawable.tab_payments else if (notice.kind == "due") R.drawable.tab_plan else R.drawable.phone_outline,
                    Blue, BlueBackground, unread = !notice.read, onRead = { if (!busy) onOpen(notice) })
            }
        }
    }
}

@Composable
private fun NoticeCard(
    title: String, message: String, stamp: String, icon: Int, tint: Color, background: Color,
    unread: Boolean = false, onRead: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = QistShapes.Card,
        color = Color.White,
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onRead).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item),
        ) {
            NoticeIcon(icon, tint, background)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stamp, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (unread) Box(Modifier.size(9.dp).background(Blue, CircleShape))
            }
        }
    }
}

@Composable
private fun NoticeIcon(icon: Int, tint: Color, background: Color, large: Boolean = false) {
    Box(Modifier.size(if (large) 72.dp else 40.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(if (large) 32.dp else 20.dp))
    }
}
