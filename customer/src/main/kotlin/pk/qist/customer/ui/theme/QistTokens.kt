package pk.qist.customer.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal object QistShapes {
    val Card = RoundedCornerShape(20.dp)
    val Dialog = RoundedCornerShape(28.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Input = RoundedCornerShape(14.dp)
    val Option = RoundedCornerShape(16.dp)
    val CodeCell = Input
    val UnreadBadge = CircleShape
    val Pill = CircleShape
}

internal object QistSpacing {
    val None = 0.dp
    val Micro = 3.dp
    val Tiny = 4.dp
    val Compact = 6.dp
    val Small = 8.dp
    val IconGap = 10.dp
    val Item = 12.dp
    val Content = 14.dp
    val Section = 16.dp
    val Screen = 20.dp
    val AuthScreen = 24.dp
    val Heading = 28.dp
}

internal object QistSize {
    val TouchTarget = 48.dp
    val Button = 52.dp
    val AuthButton = 56.dp
    val Field = 56.dp
    val OtpWidth = 48.dp
    val OtpHeight = 60.dp
    val BottomNav = 76.dp
    val StatusChip = 28.dp
    val IconSmall = 16.dp
    val IconMedium = 18.dp
    val Icon = 20.dp
    val IconLarge = 22.dp
    val IconMax = 24.dp
    val CircleSmall = 36.dp
    val Circle = 40.dp
    val CircleLarge = 44.dp
    val Avatar = 52.dp
    val HeroBadge = 64.dp
    val ConsentBadge = 56.dp
    val Logo = 104.dp
    val UnreadDot = 8.dp
    val Progress = 8.dp
    val Border = 1.dp
    val FocusBorder = 2.dp
    val Stroke = 2.dp
    val NoElevation = 0.dp
    val HomeBackdrop = 160.dp
}

// Existing placeholder geometry, pending screen-specific skeletons in milestone 3.
internal object QistSkeletonSize {
    val Header = 78.dp
    val Title = 28.dp
    val Greeting = 22.dp
    val Amount = 34.dp
    val Label = 18.dp
    val Body = 16.dp
    val Caption = 14.dp
}
