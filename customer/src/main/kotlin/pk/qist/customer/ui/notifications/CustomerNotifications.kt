package pk.qist.customer.ui.notifications

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
import pk.qist.customer.R
import pk.qist.customer.data.DashboardSample
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = QistShapes.Sheet,
        scrimColor = Navy.copy(alpha = 0.60f),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(QistSpacing.Section),
        ) {
            NoticeIcon(R.drawable.detail_bell, Blue, BlueBackground, large = true)
            Text(copy("Never miss a due date", "قسط کی تاریخ یاد رکھیں"), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                copy("Allow notifications for installment reminders and payment confirmations. This demo saves your preference on this device.", "قسط کی یاد دہانی اور ادائیگی کی تصدیق کے لیے اطلاعات کی اجازت دیں۔ یہ ڈیمو آپ کی ترجیح اسی فون پر محفوظ کرتا ہے۔"),
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CustomerBackButton(onBack)
            Text(copy("Notifications", "اطلاعات"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onReadAll, enabled = unread != 0, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(copy("Mark all read", "سب پڑھ لیے"))
            }
        }
        Section(copy("Today", "آج"))
        val payment = data.payments.first()
        NoticeCard(
            copy("Payment received", "ادائیگی موصول ہوئی"),
            copy("Rs. ${payment.amount} received for installment ${payment.number}. Thank you.", "قسط ${payment.number} کے لیے ${payment.amount} روپے موصول ہوئے۔ شکریہ۔"),
            "10:30 AM", R.drawable.detail_check, PaidGreen, PaidBackground,
            unread = unread and 1 != 0, onRead = { onRead(1) },
        )
        NoticeCard(
            copy("Installment due soon", "قسط جلد واجب ہے"),
            copy("Rs. ${data.amount} is due on ${data.dueDate}.", "${data.amount} روپے ${data.dueDate} کو واجب ہیں۔"),
            "9:00 AM", R.drawable.detail_clock, DueAmber, DueBackground,
            unread = unread and 2 != 0, onRead = { onRead(2) },
        )
        Section(copy("Earlier", "پہلے"))
        NoticeCard(
            copy("Payment overdue", "قسط کی تاریخ گزر گئی"),
            copy("Your installment of Rs. ${payment.amount} was due on ${payment.date}. Please contact ${data.retailer}.", "${payment.amount} روپے کی قسط ${payment.date} کو واجب تھی۔ ${data.retailer} سے رابطہ کریں۔"),
            payment.date, R.drawable.notice_overdue, Destructive, DestructiveBackground,
        )
        NoticeCard(
            copy("Phone unlocked", "فون ان لاک ہو گیا"),
            copy("Your phone is unlocked after your payment.", "ادائیگی کے بعد آپ کا فون ان لاک ہو گیا۔"),
            payment.date, R.drawable.notice_unlock, Blue, BlueBackground,
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title, Modifier.padding(top = 8.dp, start = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
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
            Modifier.then(if (unread) Modifier.clickable(onClick = onRead) else Modifier).padding(16.dp),
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
