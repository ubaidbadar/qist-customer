package pk.qist.customer.ui.theme

import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pk.qist.customer.R

internal val Navy = Color(0xFF0A1A3F)

internal val DeepSurface = Color(0xFF11275A)

internal val Muted = Color(0xFFA9B8DD)

internal val Cyan = Color(0xFF22D3EE)

internal val Blue = Color(0xFF2F3FE0)

internal val Canvas = Color(0xFFF5F7FC)

internal val Coral = Color(0xFFFFA383)

internal val PaidGreen = Color(0xFF08674D)

internal val PaidBackground = Color(0xFFDDF6EC)

internal val Divider = Color(0xFFE4E9F4)

internal val DueAmber = Color(0xFF795000)
internal val DueBackground = Color(0xFFFFF1D5)
internal val Destructive = Color(0xFFAA381D)
internal val DestructiveBackground = Color(0xFFFFE8E0)
internal val BlueBackground = Color(0xFFEEF1FF)
internal val InputBorder = Color(0xFFBFCBE4)
internal val BrandGradientStart = Color(0xFF06B6D4)

internal object QistShapes {
    val Card = RoundedCornerShape(22.dp)
    val Dialog = RoundedCornerShape(28.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Input = RoundedCornerShape(14.dp)
    val Option = RoundedCornerShape(16.dp)
    val CodeCell = RoundedCornerShape(13.dp)
    val UnreadBadge = RoundedCornerShape(3.dp)
}

internal object QistSpacing {
    val Screen = 20.dp
    val AuthScreen = 24.dp
    val Section = 16.dp
    val Item = 12.dp
}

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
            onSurfaceVariant = Color(0xFF4A5A80),
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
