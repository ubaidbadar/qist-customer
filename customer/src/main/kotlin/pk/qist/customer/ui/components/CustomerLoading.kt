package pk.qist.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun CustomerSkeleton() {
    Column(Modifier.fillMaxSize().background(Canvas).systemBarsPadding().padding(QistSpacing.Screen)
        .semantics { contentDescription = "Loading your account" }, verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
        Box(Modifier.fillMaxWidth().height(88.dp).background(Navy, QistShapes.Card))
        repeat(3) {
            Panel {
                Box(Modifier.fillMaxWidth(0.5f).height(20.dp).background(Divider, QistShapes.Option))
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(48.dp).background(Divider, QistShapes.Input))
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(0.7f).height(16.dp).background(Divider, QistShapes.Option))
            }
        }
    }
}

@Composable
internal fun CustomerEmptyState(message: String) {
    Panel {
        Text(copy("No records yet", "ابھی کوئی ریکارڈ نہیں"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Caption(message)
    }
}
