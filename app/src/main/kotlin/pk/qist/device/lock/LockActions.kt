package pk.qist.device.lock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pk.qist.device.R

private val TouchTarget = 56.dp

/** "Call retailer" (only when the branch has a phone number) and "Emergency call", both large and full width. */
@Composable
fun LockActions(
    branchName: String?,
    branchPhone: String?,
    onCallBranch: (phone: String) -> Unit,
    onEmergencyCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (branchPhone != null) {
            val description = stringResource(R.string.lock_call_branch_description, branchName.orEmpty(), branchPhone)
            Button(
                onClick = { onCallBranch(branchPhone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget)
                    .semantics { contentDescription = description }
            ) {
                Icon(
                    painter = painterResource(R.drawable.qist_phone),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.lock_call_branch),
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        OutlinedButton(
            onClick = onEmergencyCall,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchTarget)
        ) {
            Text(
                text = stringResource(R.string.lock_emergency_call),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
