package pk.qist.customer.ui.dashboard

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
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
internal fun CustomerScaffold(tab: CustomerTab, onTab: (CustomerTab) -> Unit, offline: Boolean = false, content: @Composable (PaddingValues) -> Unit) {
    val home = tab == CustomerTab.Home
    val window = LocalActivity.current?.window
    DisposableEffect(window, home, offline) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.isAppearanceLightStatusBars = !home || offline
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }
    val icons = listOf(R.drawable.tab_home, R.drawable.tab_payments, R.drawable.tab_plan, R.drawable.tab_profile)
    Scaffold(containerColor = Canvas,
        topBar = {
            Column {
                Spacer(Modifier.fillMaxWidth().background(if (offline) DueBackground else if (home) Navy else Canvas).statusBarsPadding())
                if (offline) Surface(color = DueBackground) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.state_offline), null, Modifier.size(18.dp), tint = DueAmber)
                        Text(copy("You are offline. Showing saved data.", "آپ آف لائن ہیں۔ محفوظ ڈیٹا دکھایا جا رہا ہے۔"),
                            Modifier.weight(1f), color = DueAmber, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
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
            if (tab == CustomerTab.Home) {
                HomeSkeleton()
            } else if (tab == CustomerTab.Plan) {
                PlanSkeleton()
            } else {
                Column(Modifier.padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
                    SkeletonLine(0.42f, 28)
                    Panel {
                        SkeletonLine(0.45f, 18); Spacer(Modifier.height(16.dp)); SkeletonLine(0.7f, 34)
                        Spacer(Modifier.height(18.dp)); SkeletonLine(0.85f, 16)
                        Spacer(Modifier.height(20.dp)); SkeletonLine(1f, 16)
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
}

@Composable
private fun HomeSkeleton() {
    HomeFrame(headerContent = {
        Column(Modifier.weight(1f)) {
            // Reserve the same line heights as the greeting and name, not just bar heights.
            Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(110.dp, 12.dp, onNavy = true)
            }
            Box(Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(180.dp, 22.dp, shape = QistShapes.CodeCell, onNavy = true)
            }
        }
        SkeletonBlock(Modifier.size(48.dp), onNavy = true)
    }) {
        Panel {
            Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(110.dp, 14.dp)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.height(40.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(200.dp, 34.dp, shape = QistShapes.CodeCell)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(170.dp, 14.dp)
            }
            Spacer(Modifier.height(16.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(8.dp))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(150.dp, 12.dp)
            }
            Spacer(Modifier.height(16.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(54.dp))
        }
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QistSpacing.Item)) {
                SkeletonBlock(Modifier.size(44.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                        SkeletonBar(150.dp, 14.dp)
                    }
                    Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                        SkeletonBar(90.dp, 12.dp)
                    }
                }
                SkeletonBlock(Modifier.width(72.dp).height(28.dp))
                SkeletonBlock(Modifier.size(16.dp))
            }
        }
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.CenterStart) {
                    SkeletonBar(150.dp, 18.dp)
                }
                SkeletonBlock(Modifier.width(70.dp).height(40.dp))
            }
            repeat(2) {
                HorizontalDivider(color = Divider)
                Box(Modifier.padding(vertical = 12.dp)) { SkeletonPaymentRow() }
            }
        }
    }
}

@Composable
private fun PlanSkeleton() {
    Column(Modifier.fillMaxWidth().padding(QistSpacing.Screen), verticalArrangement = Arrangement.spacedBy(QistSpacing.Section)) {
        Text(copy("My plan", "میرا منصوبہ"), style = MaterialTheme.typography.headlineMedium)
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(44.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBar(130.dp, 16.dp)
                    SkeletonBar(90.dp, 12.dp)
                }
            }
            Spacer(Modifier.height(28.dp))
            repeat(4) { index ->
                if (index > 0) Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SkeletonBar(90.dp, 13.dp) }
                    SkeletonBlock(Modifier.width(80.dp).height(14.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Panel {
            SkeletonBar(100.dp, 16.dp)
            Spacer(Modifier.height(24.dp))
            repeat(4) { index ->
                if (index > 0) Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(Modifier.size(36.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBar(110.dp, 14.dp)
                        SkeletonBar(80.dp, 12.dp)
                    }
                    SkeletonBlock(Modifier.width(70.dp).height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonPaymentRow() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(110.dp, 14.dp)
            }
            Box(Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
                SkeletonBar(90.dp, 12.dp)
            }
        }
        SkeletonBlock(Modifier.width(70.dp).height(14.dp))
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp, shape: Shape = CircleShape, onNavy: Boolean = false) {
    SkeletonBlock(Modifier.widthIn(max = width).fillMaxWidth().height(height), shape, onNavy)
}

@Composable
private fun SkeletonLine(fraction: Float, height: Int) {
    SkeletonBlock(Modifier.fillMaxWidth(fraction).height(height.dp))
}

@Composable
private fun SkeletonBlock(modifier: Modifier, shape: Shape = CircleShape, onNavy: Boolean = false) {
    val brush = remember { Brush.horizontalGradient(listOf(Divider, Canvas, Divider)) }
    val fill = if (onNavy) Modifier.background(Color.White.copy(alpha = 0.14f), shape) else Modifier.background(brush, shape)
    Box(modifier.then(fill))
}
