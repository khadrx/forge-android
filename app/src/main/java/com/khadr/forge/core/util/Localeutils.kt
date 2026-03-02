package com.khadr.forge.core.util

import java.text.NumberFormat
import java.util.Locale

// ─── Forge Locale Utils ───────────────────────────────────────────────────────
//
//  Usage:
//    val fmt = ForgeFormatter(isArabic = true)
//    fmt.number(1234)        → "١٬٢٣٤"
//    fmt.currency(8400.0)    → "EGP ٨٬٤٠٠"
//    fmt.percent(0.75f)      → "٧٥٪"
//    fmt.number(1234, false) → "1,234"  (force English regardless of locale)

class ForgeFormatter(val isArabic: Boolean = false) {

    private val locale: Locale get() = if (isArabic) Locale("ar") else Locale.ENGLISH

    /** Format an integer with locale-appropriate digits + grouping separator */
    fun number(value: Int): String =
        NumberFormat.getNumberInstance(locale).format(value)

    /** Format a Long */
    fun number(value: Long): String =
        NumberFormat.getNumberInstance(locale).format(value)

    /** Format a Double — no decimals */
    fun number(value: Double): String =
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }.format(value)

    /** Format a Double with explicit decimal places */
    fun number(value: Double, decimals: Int): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(value)

    /** "EGP ١٬٢٣٤" or "EGP 1,234" */
    fun currency(value: Double, symbol: String = "EGP"): String =
        "$symbol ${number(value)}"

    /** "٧٥٪" or "75%" */
    fun percent(fraction: Float): String {
        val pct = (fraction * 100).toInt()
        return if (isArabic) "${toArabicDigits(pct.toString())}٪" else "$pct%"
    }

    /** Raw string → localized digits (for edge-cases) */
    fun digits(input: String): String =
        if (isArabic) toArabicDigits(input) else input

    // ── Internal ──────────────────────────────────────────────────────────────
    private fun toArabicDigits(input: String): String {
        val arabicDigits = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
        return buildString {
            for (ch in input) {
                append(if (ch.isDigit()) arabicDigits[ch - '0'] else ch)
            }
        }
    }
}