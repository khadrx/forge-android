package com.khadr.forge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.khadr.forge.core.ui.LocalToastState
import com.khadr.forge.core.ui.ToastState
import com.khadr.forge.core.util.ForgeFormatter
import com.khadr.forge.core.util.LocalForgeFormatter

// ─── Color Schemes ────────────────────────────────────────────────────────────
private val ForgeDarkColorScheme = darkColorScheme(
    // Orange as primary
    primary              = ForgeOrange,
    onPrimary            = ForgeWhite,
    primaryContainer     = ForgeOrange.copy(alpha = 0.18f),
    onPrimaryContainer   = ForgeOrangeLight,

    secondary            = ForgeTextSecondaryDark,
    onSecondary          = ForgeBlack,
    secondaryContainer   = ForgeDarkElevated,
    onSecondaryContainer = ForgeTextSecondaryDark,

    tertiary             = ForgeTextTertiaryDark,

    // Surfaces
    background           = ForgeBlack,
    onBackground         = ForgeTextPrimaryDark,
    surface              = ForgeDark,
    onSurface            = ForgeTextPrimaryDark,
    surfaceVariant       = ForgeDarkElevated,
    onSurfaceVariant     = ForgeTextSecondaryDark,

    outline              = ForgeDarkBorder,
    outlineVariant       = ForgeTextTertiaryDark,
    error                = ForgeError
)

private val ForgeLightColorScheme = lightColorScheme(
    // Orange as primary
    primary              = ForgeOrange,
    onPrimary            = ForgeWhite,
    primaryContainer     = ForgeOrangeSubtle,
    onPrimaryContainer   = ForgeOrange,

    secondary            = ForgeTextSecondaryLight,
    onSecondary          = ForgeWhite,
    secondaryContainer   = ForgeLightElevated,
    onSecondaryContainer = ForgeTextSecondaryLight,

    tertiary             = ForgeTextTertiaryLight,

    // Surfaces — warm, not cold zinc
    background           = ForgeWhite,
    onBackground         = ForgeTextPrimaryLight,
    surface              = ForgeSurface,
    onSurface            = ForgeTextPrimaryLight,
    surfaceVariant       = ForgeLight,
    onSurfaceVariant     = ForgeTextSecondaryLight,

    outline              = ForgeLightBorder,
    outlineVariant       = ForgeTextTertiaryLight,
    error                = ForgeError
)

// ─── Neumorphic compat tokens ─────────────────────────────────────────────────
data class NeumorphicColors(
    val background      : Color,
    val shadowDark      : Color,
    val shadowLight     : Color,
    val surfaceElevated : Color
)

val LocalNeumorphicColors = staticCompositionLocalOf {
    NeumorphicColors(ForgeWhite, ForgeLightShadowD, ForgeLightShadowH, ForgeLight)
}

private val LightNeumorphicColors = NeumorphicColors(ForgeWhite, ForgeLightShadowD, ForgeLightShadowH, ForgeLight)
private val DarkNeumorphicColors  = NeumorphicColors(ForgeBlack, ForgeDarkShadowL,  ForgeDarkShadowH,  ForgeDark)

val LocalIsRtl = staticCompositionLocalOf { false }

// ─── ForgeTheme ───────────────────────────────────────────────────────────────
@Composable
fun ForgeTheme(
    darkTheme  : Boolean          = isSystemInDarkTheme(),
    rtl        : Boolean          = false,
    fontConfig : ForgeFontConfig  = ForgeFontConfig(GeistFamily, GeistFamily, GeistMonoFamily),
    textScale  : Float            = 1.0f,
    toastState : ToastState       = remember { ToastState() },
    content    : @Composable () -> Unit
) {
    val colorScheme      = if (darkTheme) ForgeDarkColorScheme else ForgeLightColorScheme
    val neumorphicColors = if (darkTheme) DarkNeumorphicColors else LightNeumorphicColors
    val layoutDirection  = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val formatter        = remember(rtl) { ForgeFormatter(isArabic = rtl) }
    val typography       = buildForgeTypography(
        fonts = fontConfig,
        scale = textScale
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalNeumorphicColors provides neumorphicColors,
        LocalLayoutDirection  provides layoutDirection,
        LocalIsRtl            provides rtl,
        LocalForgeFormatter   provides formatter,
        LocalForgeFonts       provides fontConfig,
        LocalTextScale        provides textScale,
        LocalToastState       provides toastState
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = typography,
            content     = content
        )
    }
}

// ─── Extensions ───────────────────────────────────────────────────────────────
val MaterialTheme.neumorphic: NeumorphicColors
    @Composable get() = LocalNeumorphicColors.current

val MaterialTheme.isRtl: Boolean
    @Composable get() = LocalIsRtl.current

val MaterialTheme.fmt: ForgeFormatter
    @Composable get() = LocalForgeFormatter.current