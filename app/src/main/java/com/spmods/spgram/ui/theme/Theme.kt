package com.spmods.spgram.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// Composition local for theme toggle
val LocalDarkTheme = staticCompositionLocalOf { true }

private val SPGramDark = darkColorScheme(
    primary              = Primary,
    onPrimary            = OnPrimary,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = DarkOnSurfaceVar,
    secondaryContainer   = DarkAvatarBg,
    onSecondaryContainer = DarkOnBackground,
)

private val SPGramLight = lightColorScheme(
    primary              = Primary,
    onPrimary            = OnPrimary,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVar,
    onSurfaceVariant     = LightOnSurfaceVar,
    secondaryContainer   = LightAvatarBg,
    onSecondaryContainer = LightOnBackground,
)

@Composable
fun SPGramTheme(isDark: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (isDark) SPGramDark else SPGramLight
    CompositionLocalProvider(LocalDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}

// Convenient color accessors that auto-switch with theme
val Background    @Composable get() = if (LocalDarkTheme.current) DarkBackground   else LightBackground
val Surface       @Composable get() = if (LocalDarkTheme.current) DarkSurface      else LightSurface
val SurfaceVar    @Composable get() = if (LocalDarkTheme.current) DarkSurfaceVar   else LightSurfaceVar
val OnBackground  @Composable get() = if (LocalDarkTheme.current) DarkOnBackground else LightOnBackground
val OnSurface     @Composable get() = if (LocalDarkTheme.current) DarkOnSurface    else LightOnSurface
val OnSurfaceVar  @Composable get() = if (LocalDarkTheme.current) DarkOnSurfaceVar else LightOnSurfaceVar
val AvatarBg      @Composable get() = if (LocalDarkTheme.current) DarkAvatarBg     else LightAvatarBg
val ArchiveBg     @Composable get() = if (LocalDarkTheme.current) DarkArchiveBg    else LightArchiveBg
val Divider       @Composable get() = if (LocalDarkTheme.current) DarkDivider      else LightDivider
