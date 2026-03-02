package com.khadr.forge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// ─── Text Scale CompositionLocal ─────────────────────────────────────────────
val LocalTextScale = compositionLocalOf { 1.0f }

// ─── Backward-compat aliases ─────────────────────────────────────────────────
val GeistFamily     = FontFamily.Default
val GeistMonoFamily = FontFamily.Monospace
val DmSansFamily    = GeistFamily
val SpaceMonoFamily = GeistMonoFamily

// ─── Dynamic Typography builder ───────────────────────────────────────────────
@Composable
fun buildForgeTypography(
    fonts: ForgeFontConfig = LocalForgeFonts.current,
    scale: Float           = LocalTextScale.current
): Typography {
    val s = scale  // shorthand

    fun TextUnit.sc() = (this.value * s).sp   // scale a size

    return Typography(
        // ── Display ──────────────────────────────────────────────────────────
        displayLarge = TextStyle(
            fontFamily = fonts.displayFamily, fontWeight = FontWeight.Bold,
            fontSize = 56.sp.sc(), lineHeight = 60.sp.sc(), letterSpacing = (-1.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fonts.displayFamily, fontWeight = FontWeight.Bold,
            fontSize = 44.sp.sc(), lineHeight = 48.sp.sc(), letterSpacing = (-1).sp
        ),
        displaySmall = TextStyle(
            fontFamily = fonts.displayFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp.sc(), lineHeight = 40.sp.sc(), letterSpacing = (-0.5).sp
        ),
        // ── Headline ─────────────────────────────────────────────────────────
        headlineLarge = TextStyle(
            fontFamily = fonts.displayFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp.sc(), lineHeight = 36.sp.sc(), letterSpacing = (-0.75).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fonts.displayFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp.sc(), lineHeight = 30.sp.sc(), letterSpacing = (-0.5).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fonts.displayFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp.sc(), lineHeight = 26.sp.sc(), letterSpacing = (-0.25).sp
        ),
        // ── Title ─────────────────────────────────────────────────────────────
        titleLarge = TextStyle(
            fontFamily = fonts.displayFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp.sc(), lineHeight = 24.sp.sc(), letterSpacing = (-0.2).sp
        ),
        titleMedium = TextStyle(
            fontFamily = fonts.bodyFamily, fontWeight = FontWeight.Medium,
            fontSize = 15.sp.sc(), lineHeight = 22.sp.sc(), letterSpacing = (-0.1).sp
        ),
        titleSmall = TextStyle(
            fontFamily = fonts.bodyFamily, fontWeight = FontWeight.Medium,
            fontSize = 13.sp.sc(), lineHeight = 18.sp.sc(), letterSpacing = 0.sp
        ),
        // ── Body ──────────────────────────────────────────────────────────────
        bodyLarge = TextStyle(
            fontFamily = fonts.bodyFamily, fontWeight = FontWeight.Normal,
            fontSize = 16.sp.sc(), lineHeight = 24.sp.sc(), letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fonts.bodyFamily, fontWeight = FontWeight.Normal,
            fontSize = 14.sp.sc(), lineHeight = 20.sp.sc(), letterSpacing = 0.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fonts.bodyFamily, fontWeight = FontWeight.Normal,
            fontSize = 12.sp.sc(), lineHeight = 16.sp.sc(), letterSpacing = 0.1.sp
        ),
        // ── Label ─────────────────────────────────────────────────────────────
        // Buttons, chips, tabs → Medium weight
        labelLarge = TextStyle(
            fontFamily = fonts.bodyFamily, fontWeight = FontWeight.Medium,
            fontSize = 13.sp.sc(), lineHeight = 18.sp.sc(), letterSpacing = 0.sp
        ),
        // Section headers, captions → tracked
        labelMedium = TextStyle(
            fontFamily = fonts.bodyFamily, fontWeight = FontWeight.Medium,
            fontSize = 11.sp.sc(), lineHeight = 16.sp.sc(), letterSpacing = 0.25.sp
        ),
        // Meta tags, timestamps → monospace
        labelSmall = TextStyle(
            fontFamily = fonts.monoFamily, fontWeight = FontWeight.Normal,
            fontSize = 10.sp.sc(), lineHeight = 14.sp.sc(), letterSpacing = 0.4.sp
        )
    )
}

// ─── Static fallback (for code that calls ForgeTypography directly) ──────────
val ForgeTypography: Typography get() = Typography()

// ─── Custom styles (money, stats, clock) — read LocalForgeFonts at call site ─
object ForgeTextStyles {
    val MoneyLarge = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-1.5).sp
    )
    val MoneyMedium = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.5).sp
    )
    val MoneySmall = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp
    )
    val StatNumber = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp
    )
    val ClockDisplay = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-2).sp
    )
}