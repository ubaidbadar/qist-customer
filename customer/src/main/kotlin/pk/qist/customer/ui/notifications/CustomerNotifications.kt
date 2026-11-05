package pk.qist.customer.ui.notifications

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pk.qist.customer.R
import pk.qist.customer.data.*
import pk.qist.customer.ui.components.*
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

internal enum class NoticeKind { Payment, Due, Phone }
internal data class AccountNotice(val id: String, val kind: NoticeKind, val amount: Long = 0, val date: String = "", val status: String = "")

internal fun accountNotices(data: CustomerOverview, reminders: Boolean = true): List<AccountNotice> = buildList {
    data.payments.items.take(10).forEach { add(AccountNotice("payment:${it.id}", NoticeKind.Payment, it.amount, it.receivedOn)) }
    val plan = data.plan ?: return@buildList
    if (reminders) plan.installments.filter { it.remaining > 0 && (daysUntil(it.dueDate, data.today) ?: Int.MAX_VALUE) <= 7 }.forEach {
        add(AccountNotice("${plan.id}:due:${it.number}:${it.remaining}", NoticeKind.Due, it.remaining, it.dueDate, if (it.dueDate < data.today) "overdue" else "due"))
    }
    plan.deviceLockState?.let { add(AccountNotice("${plan.id}:phone:$it", NoticeKind.Phone, status = it)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationPrompt(onAllow: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, shape = QistShapes.Sheet, scrimColor = Navy.copy(alpha = 0.60f)) {
        Column(Modifier.fillMaxWidth().padding(QistSpacing.AuthScreen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
            Text(copy("Keep track of due dates", "قسطوں کی تاریخیں یاد رکھیں"), style = MaterialTheme.typography.headlineMedium)
            Caption(copy("Show due-date notices in Qist while you use the app. This setting does not enable background push notifications.", "ایپ استعمال کرتے وقت قسط کی یاد دہانیاں دکھائیں۔ اس سے پس منظر کی پش اطلاعات فعال نہیں ہوتیں۔"))
            Pill(copy("Enable in-app reminders", "ایپ میں یاد دہانیاں فعال کریں"), onAllow)
            TextButton(onClick = onDismiss) { Text(copy("Not now", "ابھی نہیں")) }
        }
    }
}

@Composable
internal fun NotificationInbox(notices: List<AccountNotice>, readIds: Set<String>, onBack: () -> Unit, onReadAll: () -> Unit, onRead: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(pk.qist.customer.ui.theme.Canvas).systemBarsPadding(),
        contentPadding = PaddingValues(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
        item("header") { Row {
            CustomerBackButton(onBack)
            Text(copy("Notifications", "اطلاعات"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onReadAll, enabled = notices.any { it.id !in readIds }) { Text(copy("Mark all read", "سب پڑھ لیے")) }
        } }
        if (notices.isEmpty()) item("empty") { CustomerEmptyState(copy("No account notices yet.", "ابھی کوئی اطلاع نہیں۔")) }
        items(notices, key = { it.id }, contentType = { it.kind }) { notice ->
            val unread = notice.id !in readIds
            Surface(modifier = Modifier.fillMaxWidth().then(if (unread) Modifier.clickable { onRead(notice.id) } else Modifier), color = MaterialTheme.colorScheme.surface, shape = QistShapes.Card) {
                Row(Modifier.padding(QistSpacing.Section), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                    val icon = when (notice.kind) {
                        NoticeKind.Payment -> R.drawable.detail_check
                        NoticeKind.Due -> if (notice.status == "overdue") R.drawable.notice_overdue else R.drawable.detail_clock
                        NoticeKind.Phone -> if (notice.status == "unlocked") R.drawable.notice_unlock else R.drawable.notice_overdue
                    }
                    Badge(icon, Blue, BlueBackground)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                    Text(when (notice.kind) { NoticeKind.Payment -> copy("Payment received", "ادائیگی موصول ہوئی"); NoticeKind.Due -> if (notice.status == "overdue") copy("Payment overdue", "ادائیگی کی تاریخ گزر گئی") else copy("Installment due soon", "قسط جلد واجب ہے"); NoticeKind.Phone -> copy("Phone status", "فون کی حالت") }, style = MaterialTheme.typography.titleMedium)
                    if (notice.kind == NoticeKind.Phone) Caption(copy("Last acknowledged status: ${notice.status}", "آخری تصدیق شدہ حالت: ${notice.status}"))
                    else { Text(money(notice.amount)); Caption(notice.date) }
                    if (unread) Text(copy("Unread", "نہیں پڑھی گئی"), color = Blue, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
