package pk.qist.customer.ui.localization

import androidx.compose.runtime.*

internal val LocalUrdu = compositionLocalOf { false }

@Composable
internal fun copy(en: String, ur: String): String = if (LocalUrdu.current) ur else en
