package pk.qist.device.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** The Qist look: brand colours (no wallpaper-based dynamic colour) and Material 3 type, light or dark. */
@Composable
fun QistTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) QistDarkColors else QistLightColors,
        content = content
    )
}
