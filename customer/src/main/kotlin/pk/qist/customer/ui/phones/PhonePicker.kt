package pk.qist.customer.ui.phones

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.data.PlanChoice
import pk.qist.customer.ui.components.ScreenBadge
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhonePicker(phones: List<PlanChoice>, selectedId: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = QistShapes.Sheet, containerColor = CardSurface, scrimColor = SheetScrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = InputBorder, width = QistSize.Circle) },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = QistSpacing.Screen).padding(bottom = QistSpacing.Heading).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(QistSpacing.IconGap),
        ) {
            Text(copy("My phones", "میرے فون"), style = MaterialTheme.typography.headlineSmall)
            Text(copy("Choose a phone to see its plan and payments.", "منصوبہ اور ادائیگیاں دیکھنے کے لیے فون منتخب کریں۔"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("active", "completed", "cancelled").forEach { groupName ->
                val completed = groupName == "completed"
                val cancelled = groupName == "cancelled"
                val group = phones.filter { when (groupName) {
                    "completed" -> it.completed
                    "cancelled" -> it.status == "cancelled"
                    else -> it.status != "completed" && it.status != "cancelled"
                } }
                if (group.isNotEmpty()) {
                    Text(if (completed) copy("Completed (history)", "مکمل (سابقہ)") else if (cancelled) copy("Cancelled plans", "منسوخ منصوبے") else copy("Active plans", "فعال منصوبے"),
                        Modifier.padding(top = QistSpacing.IconGap, start = QistSpacing.Tiny), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                    group.forEach { phone ->
                        key(phone.id) {
                            val selected = phone.id == selectedId
                            Surface(shape = QistShapes.Option, color = if (selected) BlueBackground else CardSurface,
                                border = BorderStroke(if (selected) QistSize.FocusBorder else QistSize.Border, if (selected) Blue else Divider)) {
                                Row(
                                    Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(phone.id) }).padding(QistSpacing.Content),
                                    horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item), verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ScreenBadge(if (completed) R.drawable.detail_check else R.drawable.phone_outline,
                                        if (completed) Navy else Blue, if (selected) CardSurface else if (completed) Divider else BlueBackground, QistSize.Circle)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QistSpacing.Micro)) {
                                        Text(phone.model, style = MaterialTheme.typography.titleMedium)
                                        Text(when {
                                            cancelled -> copy("Plan cancelled", "منصوبہ منسوخ ہو گیا")
                                            completed && phone.paidOffDate != "—" -> copy("Paid off ${phone.paidOffDate} · ${phone.total} installments", "${phone.paidOffDate} کو مکمل · ${phone.total} اقساط")
                                            completed -> copy("${phone.total} installments · Completed", "${phone.total} اقساط · مکمل")
                                            phone.dueDate == "—" -> copy("No upcoming installment", "کوئی آئندہ قسط نہیں")
                                            else -> copy("Next due ${phone.dueDate} · Rs. ${phone.amount}", "اگلی قسط ${phone.dueDate} · روپے ${phone.amount}")
                                        },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                        PlanStatusBadge(phone.status, phone.days.takeIf { phone.dueDate != "—" })
                                    }
                                    RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Blue, unselectedColor = InputBorder))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlanStatusBadge(status: String, days: Int?) {
    val completed = status == "completed"
    val cancelled = status == "cancelled"
    val overdue = status == "defaulted" || (status == "active" && days != null && days < 0)
    val due = status == "active" && days != null && days in 0..3
    val color = when { overdue -> Danger; due -> DueAmber; completed || cancelled -> Navy; else -> PaidGreen }
    val background = when { overdue -> DestructiveBackground; due -> DueBackground; completed || cancelled -> NeutralBackground; else -> PaidBackground }
    Surface(color = background, shape = QistShapes.Pill) {
        Row(Modifier.heightIn(min = QistSize.StatusChip).padding(horizontal = QistSpacing.IconGap, vertical = QistSpacing.Tiny), horizontalArrangement = Arrangement.spacedBy(QistSpacing.Compact), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(if (due || overdue) R.drawable.detail_clock else R.drawable.detail_check), null, Modifier.size(QistSize.IconSmall), tint = color)
            Text(when {
                completed -> copy("Completed", "مکمل")
                cancelled -> copy("Cancelled", "منسوخ")
                overdue -> copy("Overdue", "تاخیر")
                due -> copy("Due soon", "جلد واجب")
                else -> copy("Active", "فعال")
            }, color = color, style = MaterialTheme.typography.labelMedium)
        }
    }
}
