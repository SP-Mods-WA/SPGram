package com.spmods.spgram.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val SPGramColorScheme = darkColorScheme(
    primary             = Primary,
    onPrimary           = OnPrimary,
    background          = Background,
    onBackground        = OnBackground,
    surface             = Surface,
    onSurface           = OnSurface,
    surfaceVariant      = SurfaceVar,
    onSurfaceVariant    = OnSurfaceVar,
    secondaryContainer  = AvatarBg,
    onSecondaryContainer = OnBackground,
)

@Composable
fun SPGramTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SPGramColorScheme,
        typography  = Typography,
        content     = content
    )
}
