package pk.qist.device.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF0B1F4B)
private val Blue = Color(0xFF2F3FE0)
private val LightBlue = Color(0xFF9CA8FF)
private val Amber = Color(0xFFFFB020)
private val Paper = Color(0xFFF5F8FD)
private val Night = Color(0xFF0B1226)
private val InkRaised = Color(0xFF141D37)

/** Blue on paper for the light theme. Amber is only ever a background behind Ink text, for contrast. */
val QistLightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = LightBlue,
    onPrimaryContainer = Ink,
    tertiaryContainer = Amber,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink
)

/** Light blue on navy for the dark theme. */
val QistDarkColors = darkColorScheme(
    primary = LightBlue,
    onPrimary = Ink,
    primaryContainer = Blue,
    onPrimaryContainer = Color.White,
    tertiaryContainer = Amber,
    onTertiaryContainer = Ink,
    background = Night,
    onBackground = Color.White,
    surface = InkRaised,
    onSurface = Color.White
)

val QistLockColors = darkColorScheme(
    primary = Color(0xFF22D0E8),
    onPrimary = Color(0xFF071831),
    background = Color(0xFF0C1A3C),
    onBackground = Color.White,
    surface = Color(0xFF132754),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFAAB9DB),
    tertiary = Color(0xFFFFA283),
    outline = Color(0xFF758CC7),
    outlineVariant = Color(0xFF30446E)
)
