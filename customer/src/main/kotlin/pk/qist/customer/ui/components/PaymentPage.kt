package pk.qist.customer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import pk.qist.customer.ui.theme.*

@Composable
internal fun PaymentPage(title: String, onBack: () -> Unit, actions: @Composable ColumnScope.() -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(QistSpacing.Screen)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
            CustomerBackButton(onBack)
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = QistSpacing.Section), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section), content = body)
        Column(verticalArrangement = Arrangement.spacedBy(QistSpacing.Item), content = actions)
    }
}
