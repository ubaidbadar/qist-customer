package pk.qist.customer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pk.qist.customer.R
import pk.qist.customer.ui.theme.Blue
import pk.qist.customer.ui.theme.BrandGradientStart
import pk.qist.customer.ui.theme.Navy

/** Local session restoration has no account content to represent with a skeleton. */
@Composable
internal fun CustomerStartup() {
    CustomerSystemBars(lightIcons = true)
    Box(Modifier.fillMaxSize().background(Navy).systemBarsPadding(), contentAlignment = Alignment.Center) {
        CustomerBrand()
    }
}

@Composable
internal fun CustomerBrand() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(104.dp).background(Brush.linearGradient(listOf(BrandGradientStart, Blue)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(20.dp))
        Text("Qist", color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}
