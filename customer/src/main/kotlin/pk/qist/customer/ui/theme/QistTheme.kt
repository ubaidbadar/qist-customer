package pk.qist.customer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val colors = lightColorScheme(
    primary = Blue,
    onPrimary = CardSurface,
    primaryContainer = BlueBackground,
    onPrimaryContainer = Navy,
    secondary = BrandCyan,
    onSecondary = AccentText,
    secondaryContainer = BlueBackground,
    onSecondaryContainer = Navy,
    tertiary = PaidGreen,
    onTertiary = CardSurface,
    tertiaryContainer = PaidBackground,
    onTertiaryContainer = PaidGreen,
    background = Canvas,
    onBackground = Navy,
    surface = CardSurface,
    onSurface = Navy,
    surfaceContainerLowest = CardSurface,
    surfaceContainerLow = CardSurface,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = CardSurface,
    surfaceContainerHighest = BlueBackground,
    onSurfaceVariant = SecondaryText,
    surfaceTint = Clear,
    outline = InputOutline,
    outlineVariant = Divider,
    error = Danger,
    onError = CardSurface,
    errorContainer = DestructiveBackground,
    onErrorContainer = Danger,
    inverseSurface = Navy,
    inverseOnSurface = CardSurface,
    inversePrimary = Cyan,
    scrim = Navy,
)

private val shapes = Shapes(
    extraSmall = QistShapes.Input,
    small = QistShapes.Input,
    medium = QistShapes.Option,
    large = QistShapes.Card,
    extraLarge = QistShapes.Dialog,
)

@Composable
internal fun QistTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = QistTypography, shapes = shapes, content = content)
}
