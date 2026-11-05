package pk.qist.customer.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat

@Composable
internal fun CustomerSystemBars(lightIcons: Boolean) {
    val window = LocalActivity.current?.window
    DisposableEffect(window, lightIcons) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.isAppearanceLightStatusBars = !lightIcons
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }
}
