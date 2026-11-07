package pk.qist.customer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pk.qist.customer.R

private val inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

private val amount = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp)
private val tabTitle = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
private val pushedTitle = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
private val section = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp)
private val body = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
private val secondary = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
private val caption = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)

internal val QistTypography = Typography(
    displayLarge = amount,
    displayMedium = amount,
    displaySmall = tabTitle,
    headlineLarge = amount,
    headlineMedium = tabTitle,
    headlineSmall = pushedTitle,
    titleLarge = section,
    titleMedium = body.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = secondary.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = body,
    bodyMedium = secondary,
    bodySmall = caption,
    labelLarge = body.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = caption,
    labelSmall = caption,
)
