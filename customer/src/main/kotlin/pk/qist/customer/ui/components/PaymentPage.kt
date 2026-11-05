package pk.qist.customer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.*
import pk.qist.customer.R
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.Canvas
import pk.qist.customer.ui.theme.Navy

@Composable
internal fun PaymentPage(title: String, onBack: () -> Unit, actions: @Composable ColumnScope.() -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledIconButton(onClick = onBack, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Navy)) {
                Icon(painterResource(R.drawable.notice_back), contentDescription = copy("Back", "واپس"))
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = body)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = actions)
    }
}
