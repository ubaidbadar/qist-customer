package pk.qist.device.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pk.qist.device.R
import pk.qist.device.ui.theme.QistLockColors

@Composable
fun LockScreenContent(
    screen: LockScreenUi,
    onCallBranch: (phone: String) -> Unit,
    onEmergencyCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = QistLockColors) {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    LockHeader(screen)
                    DueSummary(screen)
                    LockActions(
                        branchName = screen.branchName,
                        branchPhone = screen.branchPhone,
                        onCallBranch = onCallBranch,
                        onEmergencyCall = onEmergencyCall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (screen.amountDue != null) {
                        Text(
                            text = stringResource(R.string.lock_payment_footer),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockHeader(screen: LockScreenUi) {
    val badge = painterResource(R.drawable.qist_badge)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(painter = badge, contentDescription = null, modifier = Modifier.size(32.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(painter = badge, contentDescription = null, modifier = Modifier.size(104.dp))
        Text(
            text = stringResource(R.string.lock_title),
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        val description = MaterialTheme.typography.bodyLarge
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = if (screen.amountDue == null) screen.message else stringResource(R.string.lock_payment_message),
            textAlign = TextAlign.Center,
            color = muted,
            style = description
        )
        if (screen.amountDue != null) {
            Text(
                text = stringResource(R.string.lock_payment_message_urdu),
                textAlign = TextAlign.Center,
                color = muted,
                style = description.copy(textDirection = TextDirection.Rtl)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LockScreenContentPreview() {
    LockScreenContent(
        screen = LockScreenUi(
            message = "",
            amountDue = "Rs 15,000",
            nextDueDate = "12 Oct 2026",
            branchName = "Saddar",
            branchPhone = "0300 1234567",
            overdueDays = 3
        ),
        onCallBranch = {},
        onEmergencyCall = {}
    )
}
