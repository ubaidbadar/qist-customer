package pk.qist.device.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pk.qist.device.R

@Composable
fun DueSummary(screen: LockScreenUi, modifier: Modifier = Modifier) {
    val overdue = screen.overdueDays?.let { pluralStringResource(R.plurals.lock_days, it, it) }
    val rows = listOfNotNull(
        screen.amountDue?.let { R.string.lock_amount_due to it },
        screen.nextDueDate?.let { R.string.lock_due_date to it },
        overdue?.let { R.string.lock_overdue_by to it },
        screen.branchName?.let { R.string.lock_retailer to it }
    )
    if (rows.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface, contentColor = colors.onSurface)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(label),
                        modifier = Modifier.weight(0.9f),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = value,
                        modifier = Modifier.weight(1.1f),
                        color = if (label == R.string.lock_overdue_by) colors.tertiary else colors.onSurface,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
