package pk.qist.customer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

internal val Navy = Color(0xFF0A1A3F)
internal val DeepSurface = Color(0xFF11275A)
internal val Muted = Color(0xFFA9B8DD)
internal val Cyan = Color(0xFF22D3EE)
internal val Blue = Color(0xFF2F3FE0)
internal val Canvas = Color(0xFFF5F7FC)
internal val Coral = Color(0xFFFFA383)

private val inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

@Composable
internal fun QistTheme(content: @Composable () -> Unit) {
    val type = Typography()
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            background = Canvas,
            onBackground = Navy,
            surface = Color.White,
            onSurface = Navy,
            onSurfaceVariant = Color(0xFF46577F),
        ),
        typography = type.copy(
            headlineLarge = type.headlineLarge.copy(fontFamily = inter, fontWeight = FontWeight.Bold),
            headlineMedium = type.headlineMedium.copy(fontFamily = inter, fontWeight = FontWeight.Bold),
            titleLarge = type.titleLarge.copy(fontFamily = inter, fontWeight = FontWeight.Bold),
            titleMedium = type.titleMedium.copy(fontFamily = inter, fontWeight = FontWeight.SemiBold),
            bodyLarge = type.bodyLarge.copy(fontFamily = inter),
            bodyMedium = type.bodyMedium.copy(fontFamily = inter),
            labelLarge = type.labelLarge.copy(fontFamily = inter, fontWeight = FontWeight.SemiBold),
            labelMedium = type.labelMedium.copy(fontFamily = inter),
        ),
        content = content,
    )
}
