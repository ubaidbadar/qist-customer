package pk.qist.customer.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pk.qist.customer.ui.theme.Canvas

@Immutable
internal data class CustomerLayout(val screenPadding: Dp = 20.dp, val authPadding: Dp = 24.dp, val stackDetails: Boolean = false)
internal val LocalCustomerLayout = staticCompositionLocalOf { CustomerLayout() }

/** Bound content, rather than overriding density or the customer's accessibility font size. */
@Composable
internal fun CustomerViewport(authentication: Boolean, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Canvas), contentAlignment = Alignment.TopCenter) {
        val narrow = maxWidth < 360.dp
        val layout = CustomerLayout(
            screenPadding = if (narrow) 16.dp else 20.dp,
            authPadding = if (narrow) 16.dp else 24.dp,
            stackDetails = narrow || LocalDensity.current.fontScale >= 1.3f,
        )
        CompositionLocalProvider(LocalCustomerLayout provides layout) {
            Box(Modifier.widthIn(max = if (authentication) 600.dp else 840.dp).fillMaxSize()) { content() }
        }
    }
}
