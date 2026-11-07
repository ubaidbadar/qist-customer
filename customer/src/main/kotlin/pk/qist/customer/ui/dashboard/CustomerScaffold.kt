package pk.qist.customer.ui.dashboard

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import pk.qist.customer.R
import pk.qist.customer.ui.CustomerTab
import pk.qist.customer.ui.components.Panel
import pk.qist.customer.ui.localization.copy
import pk.qist.customer.ui.theme.*

@Composable
internal fun tabTitle(tab: CustomerTab): String = when (tab) {
    CustomerTab.Home -> copy("Home", "گھر")
    CustomerTab.Payments -> copy("Payments", "ادائیگیاں")
    CustomerTab.Plan -> copy("Plan", "منصوبہ")
    CustomerTab.Profile -> copy("Profile", "پروفائل")
}

@Composable
internal fun CustomerScaffold(tab: CustomerTab, onTab: (CustomerTab) -> Unit, content: @Composable (PaddingValues) -> Unit) {
    val home = tab == CustomerTab.Home
    val window = LocalActivity.current?.window
    DisposableEffect(window, home) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.isAppearanceLightStatusBars = !home
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }
    val icons = listOf(R.drawable.tab_home, R.drawable.tab_payments, R.drawable.tab_plan, R.drawable.tab_profile)
    Scaffold(containerColor = Canvas,
        topBar = { Spacer(Modifier.fillMaxWidth().background(if (home) Navy else Canvas).statusBarsPadding()) },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                CustomerTab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(selected = tab == item, onClick = { onTab(item) },
                        icon = { Icon(painterResource(icons[index]), null, Modifier.size(24.dp)) },
                        label = { Text(tabTitle(item), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, selectedTextColor = Blue,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = Color.Transparent))
                }
            }
        }, content = content)
}

/** Static placeholders keep the approved card geometry and respect reduced-motion preferences. */
@Composable
internal fun CustomerLoading(tab: CustomerTab, onTab: (CustomerTab) -> Unit) {
    val label = copy("Loading your account", "آپ کا اکاؤنٹ لوڈ ہو رہا ہے")
    CustomerScaffold(tab, onTab) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).clearAndSetSemantics {
            contentDescription = label; liveRegion = LiveRegionMode.Polite
        }) {
            if (tab == CustomerTab.Home) Box(Modifier.fillMaxWidth().height(78.dp).background(Navy).padding(20.dp)) {
                SkeletonLine(0.6f, 22)
            }
            Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                if (tab != CustomerTab.Home) SkeletonLine(0.42f, 28)
                Panel {
                    SkeletonLine(0.45f, 18); Spacer(Modifier.height(16.dp)); SkeletonLine(0.7f, 34)
                    Spacer(Modifier.height(18.dp)); SkeletonLine(0.85f, 16)
                    Spacer(Modifier.height(20.dp)); SkeletonLine(1f, if (tab == CustomerTab.Home) 54 else 16)
                }
                Panel { SkeletonLine(0.7f, 18); Spacer(Modifier.height(12.dp)); SkeletonLine(0.45f, 14) }
                Panel {
                    repeat(3) { index ->
                        if (index > 0) Spacer(Modifier.height(20.dp))
                        SkeletonLine(0.8f, 18); Spacer(Modifier.height(8.dp)); SkeletonLine(0.55f, 14)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(fraction: Float, height: Int) {
    Box(Modifier.fillMaxWidth(fraction).height(height.dp).background(Divider, QistShapes.CodeCell))
}

@Composable
internal fun CustomerLoadError(tab: CustomerTab, onTab: (CustomerTab) -> Unit, message: String?, onRetry: () -> Unit, onLogout: () -> Unit) {
    CustomerScaffold(tab, onTab) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            Panel {
                Text(copy("Could not load your account", "آپ کا اکاؤنٹ لوڈ نہیں ہو سکا"), style = MaterialTheme.typography.titleLarge)
                Text(message ?: copy("Check your connection and try again.", "انٹرنیٹ چیک کر کے دوبارہ کوشش کریں۔"), Modifier.padding(vertical = 12.dp))
                Button(onClick = onRetry) { Text(copy("Try again", "دوبارہ کوشش کریں")) }
                TextButton(onClick = onLogout) { Text(copy("Sign out", "لاگ آؤٹ")) }
            }
        }
    }
}
