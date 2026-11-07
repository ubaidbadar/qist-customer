package pk.qist.customer.ui.notifications

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
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
import pk.qist.customer.ui.components.CustomerBackButton
import pk.qist.customer.ui.components.Pill
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationPrompt(onAllow: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface,
        shape = QistShapes.Sheet,
        scrimColor = SheetScrim,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = QistSpacing.AuthScreen, end = QistSpacing.AuthScreen, bottom = QistSpacing.AuthScreen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(QistSpacing.Section),
        ) {
            NoticeIcon(R.drawable.detail_bell, Blue, BlueBackground, large = true)
            Text(copy("Never miss a due date", "قسط کی تاریخ یاد رکھیں"), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                copy("Remote installment reminders and payment confirmations are not available in this version.", "اس ورژن میں قسط کی ریموٹ یاد دہانی اور ادائیگی کی تصدیق دستیاب نہیں۔"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(QistSpacing.Tiny))
            Pill(copy("Allow notifications", "اطلاعات کی اجازت دیں"), onAllow)
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = QistSize.TouchTarget)) {
                Text(copy("Not now", "ابھی نہیں"))
            }
        }
    }
}

@Composable
internal fun NotificationInbox(
    data: DashboardSample,
    unread: Int,
    onBack: () -> Unit,
    onReadAll: () -> Unit,
    onRead: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Canvas).systemBarsPadding().verticalScroll(rememberScrollState()).padding(QistSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(QistSpacing.Item),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Small)) {
            CustomerBackButton(onBack)
            Text(copy("Notifications", "اطلاعات"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onReadAll, enabled = unread != 0, contentPadding = PaddingValues(horizontal = QistSpacing.Tiny)) {
                Text(copy("Mark all read", "سب پڑھ لیے"))
            }
        }
        Text(copy("No notifications available. Check Home and Payments for your latest account information.", "کوئی اطلاع دستیاب نہیں۔ تازہ معلومات کے لیے ہوم اور ادائیگیاں دیکھیں۔"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Section(title: String) {
    Text(title, Modifier.padding(top = QistSpacing.Small, start = QistSpacing.Tiny), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun NoticeCard(
    title: String, message: String, stamp: String, icon: Int, tint: Color, background: Color,
    unread: Boolean = false, onRead: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = QistShapes.Card,
        color = CardSurface,
    ) {
        Row(
            Modifier.then(if (unread) Modifier.clickable(onClick = onRead) else Modifier).padding(QistSpacing.Section),
            horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item),
        ) {
            NoticeIcon(icon, tint, background)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QistSpacing.Tiny)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(QistSpacing.Small)) {
                Text(stamp, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (unread) Box(Modifier.size(QistSize.UnreadDot).background(Blue, QistShapes.Pill))
            }
        }
    }
}

@Composable
private fun NoticeIcon(icon: Int, tint: Color, background: Color, large: Boolean = false) {
    Box(Modifier.size(if (large) QistSize.HeroBadge else QistSize.Circle).background(background, QistShapes.Pill), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(if (large) QistSize.IconMax else QistSize.Icon))
    }
}
