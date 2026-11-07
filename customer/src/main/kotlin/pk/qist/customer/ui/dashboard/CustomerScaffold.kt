package pk.qist.customer.ui.dashboard

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
            NavigationBar(modifier = Modifier.heightIn(min = QistSize.BottomNav), containerColor = CardSurface, tonalElevation = QistSize.NoElevation) {
                CustomerTab.entries.forEachIndexed { index, item ->
                    NavigationBarItem(selected = tab == item, onClick = { onTab(item) },
                        icon = { Icon(painterResource(icons[index]), null, Modifier.size(QistSize.IconMax)) },
                        label = { Text(tabTitle(item), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, selectedTextColor = Blue,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = Clear))
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
            if (tab == CustomerTab.Home) Box(Modifier.fillMaxWidth().height(QistSkeletonSize.Header).background(Navy).padding(QistSpacing.Screen)) {
                SkeletonLine(0.6f, QistSkeletonSize.Greeting)
            }
            Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                if (tab != CustomerTab.Home) SkeletonLine(0.42f, QistSkeletonSize.Title)
                Panel {
                    SkeletonLine(0.45f, QistSkeletonSize.Label); Spacer(Modifier.height(QistSpacing.Section)); SkeletonLine(0.7f, QistSkeletonSize.Amount)
                    Spacer(Modifier.height(QistSpacing.Section)); SkeletonLine(0.85f, QistSkeletonSize.Body)
                    Spacer(Modifier.height(QistSpacing.Screen)); SkeletonLine(1f, if (tab == CustomerTab.Home) QistSize.Button else QistSkeletonSize.Body)
                }
                Panel { SkeletonLine(0.7f, QistSkeletonSize.Label); Spacer(Modifier.height(QistSpacing.Item)); SkeletonLine(0.45f, QistSkeletonSize.Caption) }
                Panel {
                    repeat(3) { index ->
                        if (index > 0) Spacer(Modifier.height(QistSpacing.Screen))
                        SkeletonLine(0.8f, QistSkeletonSize.Label); Spacer(Modifier.height(QistSpacing.Small)); SkeletonLine(0.55f, QistSkeletonSize.Caption)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(fraction: Float, height: Dp) {
    Box(Modifier.fillMaxWidth(fraction).height(height).background(Divider, QistShapes.CodeCell))
}

@Composable
internal fun CustomerLoadError(tab: CustomerTab, onTab: (CustomerTab) -> Unit, message: String?, onRetry: () -> Unit, onLogout: () -> Unit) {
    CustomerScaffold(tab, onTab) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(QistSpacing.AuthScreen), verticalArrangement = Arrangement.Center) {
            Panel {
                Text(copy("Could not load your account", "آپ کا اکاؤنٹ لوڈ نہیں ہو سکا"), style = MaterialTheme.typography.titleLarge)
                Text(message ?: copy("Check your connection and try again.", "انٹرنیٹ چیک کر کے دوبارہ کوشش کریں۔"), Modifier.padding(vertical = QistSpacing.Item))
                Button(onClick = onRetry) { Text(copy("Try again", "دوبارہ کوشش کریں")) }
                TextButton(onClick = onLogout) { Text(copy("Sign out", "لاگ آؤٹ")) }
            }
        }
    }
}
