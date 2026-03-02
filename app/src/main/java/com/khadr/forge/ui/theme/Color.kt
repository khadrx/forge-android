package com.khadr.forge.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════════
//  Forge Color System v2  —  Orange Accent + Warm Neutral
//
//  Inspired by the Forge logo: orange #FF5500 on crisp white
//  Design feel: Jira / Duolingo — structured, readable, not cold
//
//  Primary accent: Orange  (buttons, active states, progress, badges)
//  Surface:        Warm white / warm dark  (not cold zinc)
//  Text:           High-contrast for readability
// ══════════════════════════════════════════════════════════════════════════════

// ── Brand Orange ──────────────────────────────────────────────────────────────
val ForgeOrange        = Color(0xFFFF600A)   // logo orange — main accent
val ForgeOrangeLight   = Color(0xFFFF7833)   // hover / pressed state
val ForgeOrangeDim     = Color(0xFFFF5500).copy(alpha = 0.15f)   // tinted backgrounds
val ForgeOrangeSubtle  = Color(0xFFFFF1E8)   // very light tint on white bg

// ── Light surfaces (warm white, not cold) ──────────────────────────────────────
val ForgeWhite         = Color(0xFFFFFFFF)
val ForgeSurface       = Color(0xFFF7F7F8)   // slightly warm off-white — card bg
val ForgeLight         = Color(0xFFF0F0F2)   // input / chip bg
val ForgeLightElevated = Color(0xFFE8E8EB)   // dividers, borders
val ForgeLightBorder   = Color(0xFFE2E2E6)   // card borders — a touch warmer

// ── Dark surfaces (warm dark, not pure zinc) ───────────────────────────────────
val ForgeBlack         = Color(0xFF0F0F11)   // bg — slightly warmer than pure black
val ForgeDark          = Color(0xFF1A1A1E)   // card bg
val ForgeDarkElevated  = Color(0xFF252529)   // input / chip bg
val ForgeDarkBorder    = Color(0xFF35353C)   // card borders
val ForgeDarkSurface   = Color(0xFF1A1A1E)

// ── Text — Light theme ─────────────────────────────────────────────────────────
val ForgeTextPrimaryLight   = Color(0xFF111114)   // near-black, slightly warm
val ForgeTextSecondaryLight = Color(0xFF6B6B78)   // medium gray
val ForgeTextTertiaryLight  = Color(0xFFA0A0AE)   // light gray

// ── Text — Dark theme ──────────────────────────────────────────────────────────
val ForgeTextPrimaryDark    = Color(0xFFF5F5F7)   // warm near-white
val ForgeTextSecondaryDark  = Color(0xFF9898A8)   // warm medium gray
val ForgeTextTertiaryDark   = Color(0xFF55555F)   // dark gray

// ── Semantic ───────────────────────────────────────────────────────────────────
val ForgeError   = Color(0xFFEF4444)   // red-500
val ForgeSuccess = Color(0xFF22C55E)   // green-500
val ForgeWarning = Color(0xFFF59E0B)   // amber-500
val ForgeInfo    = Color(0xFF3B82F6)   // blue-500

// ── Budget category colors ─────────────────────────────────────────────────────
val ForgeIncome  = Color(0xFF22C55E)   // green
val ForgeExpense = ForgeOrange          // orange (brand)
val ForgeSavings = Color(0xFF3B82F6)   // blue

// ── Category badge colors (per category) ──────────────────────────────────────
val CatFood      = Color(0xFFFF5500)   // orange  (brand)
val CatTransport = Color(0xFF3B82F6)   // blue
val CatShopping  = Color(0xFFA855F7)   // purple
val CatHealth    = Color(0xFF22C55E)   // green
val CatBills     = Color(0xFFF59E0B)   // amber
val CatSalary    = Color(0xFF22C55E)   // green
val CatFreelance = Color(0xFF06B6D4)   // cyan
val CatOther     = Color(0xFF6B7280)   // gray

// ── Budget allocation zone colors (50/30/20 rule) ─────────────────────────────
val ZoneNeeds    = Color(0xFF3B82F6)   // blue  — Needs  (50%)
val ZoneWants    = Color(0xFFA855F7)   // purple — Wants (30%)
val ZoneSavings  = Color(0xFF22C55E)   // green  — Savings (20%)
val ZoneDanger   = Color(0xFFEF4444)   // red — over budget

// ── Legacy neumorphic (kept for compile compat) ────────────────────────────────
val ForgeDarkShadowL   = Color(0xFF080809)
val ForgeDarkShadowH   = Color(0xFF222227)
val ForgeLightShadowD  = Color(0xFFD0D0D4)
val ForgeLightShadowH  = Color(0xFFFFFFFF)