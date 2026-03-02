package com.khadr.forge.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.khadr.forge.R

// ─── Font Choice ──────────────────────────────────────────────────────────────
enum class AppFont(val displayNameEn: String, val displayNameAr: String) {
    SYSTEM  ("System Default",  "خط النظام"),
    GEIST   ("Geist + IBM Plex","Geist + IBM Plex"),
    CAIRO   ("Cairo",           "القاهرة"),
    TAJAWAL ("Tajawal",         "تجوال"),
}

// ─── CompositionLocal ─────────────────────────────────────────────────────────
data class ForgeFontConfig(
    val bodyFamily   : FontFamily,   // used for bodyMedium, bodyLarge, bodySmall
    val displayFamily: FontFamily,   // used for headlines, titles
    val monoFamily   : FontFamily,   // used for numbers, money, clock
)

val LocalForgeFonts = compositionLocalOf {
    ForgeFontConfig(
        bodyFamily    = FontFamily.Default,
        displayFamily = FontFamily.Default,
        monoFamily    = FontFamily.Monospace
    )
}

// ─── Font Builders ────────────────────────────────────────────────────────────
//
// Font files expected in res/font/:
//   GEIST:   geist_regular.ttf, geist_medium.ttf, geist_semibold.ttf, geist_bold.ttf
//            (download: github.com/vercel/geist-font)
//   IBM PLEX ARABIC: ibm_plex_arabic_regular.ttf, ibm_plex_arabic_medium.ttf,
//            ibm_plex_arabic_semibold.ttf, ibm_plex_arabic_bold.ttf
//            (download: github.com/IBM/plex — fonts/IBM-Plex-Arabic/fonts/complete/ttf)
//   CAIRO:   cairo_regular.ttf, cairo_medium.ttf, cairo_semibold.ttf, cairo_bold.ttf
//            (download: fonts.google.com/specimen/Cairo)
//   TAJAWAL: tajawal_regular.ttf, tajawal_medium.ttf, tajawal_bold.ttf
//            (download: fonts.google.com/specimen/Tajawal)
//
// Until files are added → falls back to system fonts silently.

fun buildFontConfig(choice: AppFont, isArabic: Boolean): ForgeFontConfig {
    return when (choice) {
        AppFont.SYSTEM -> ForgeFontConfig(
            bodyFamily    = FontFamily.Default,
            displayFamily = FontFamily.Default,
            monoFamily    = FontFamily.Monospace
        )

        AppFont.GEIST -> if (!isArabic) {
            // English: Geist for display, Geist Mono for numbers
            val geist = tryFontFamily(
                Font(R.font.geist_regular,  FontWeight.Normal),
                Font(R.font.geist_medium,   FontWeight.Medium),
                Font(R.font.geist_semibold, FontWeight.SemiBold),
                Font(R.font.geist_bold,     FontWeight.Bold),
            )
            val geistMono = tryFontFamily(
                Font(R.font.geist_mono_regular, FontWeight.Normal),
                Font(R.font.geist_mono_bold,    FontWeight.Bold),
            )
            ForgeFontConfig(
                bodyFamily    = geist ?: FontFamily.Default,
                displayFamily = geist ?: FontFamily.Default,
                monoFamily    = geistMono ?: FontFamily.Monospace
            )
        } else {
            // Arabic: IBM Plex Arabic for text, Geist Mono for numbers
            val ibmPlex = tryFontFamily(
                Font(R.font.ibm_plex_arabic_regular,  FontWeight.Normal),
                Font(R.font.ibm_plex_arabic_medium,   FontWeight.Medium),
                Font(R.font.ibm_plex_arabic_semibold, FontWeight.SemiBold),
                Font(R.font.ibm_plex_arabic_bold,     FontWeight.Bold),
            )
            ForgeFontConfig(
                bodyFamily    = ibmPlex ?: FontFamily.Default,
                displayFamily = ibmPlex ?: FontFamily.Default,
                monoFamily    = FontFamily.Monospace
            )
        }

        AppFont.CAIRO -> {
            val cairo = tryFontFamily(
                Font(R.font.cairo_regular,  FontWeight.Normal),
                Font(R.font.cairo_medium,   FontWeight.Medium),
                Font(R.font.cairo_semibold, FontWeight.SemiBold),
                Font(R.font.cairo_bold,     FontWeight.Bold),
            )
            ForgeFontConfig(
                bodyFamily    = cairo ?: FontFamily.Default,
                displayFamily = cairo ?: FontFamily.Default,
                monoFamily    = FontFamily.Monospace
            )
        }

        AppFont.TAJAWAL -> {
            val tajawal = tryFontFamily(
                Font(R.font.tajawal_regular, FontWeight.Normal),
                Font(R.font.tajawal_medium,  FontWeight.Medium),
                Font(R.font.tajawal_bold,    FontWeight.Bold),
            )
            ForgeFontConfig(
                bodyFamily    = tajawal ?: FontFamily.Default,
                displayFamily = tajawal ?: FontFamily.Default,
                monoFamily    = FontFamily.Monospace
            )
        }
    }
}

/** Tries to build a FontFamily — returns null if any font resource is missing. */
private fun tryFontFamily(vararg fonts: Font): FontFamily? {
    return try {
        FontFamily(*fonts)
    } catch (e: Throwable) {
        null   // font files not added yet → caller falls back to system
    }
}