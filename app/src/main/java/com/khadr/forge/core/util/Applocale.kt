package com.khadr.forge.core.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

// ─── Composition Local for Forge locale ──────────────────────────────────────
//
//  Provides ForgeFormatter down the tree so any Composable can call:
//
//    val fmt = LocalForgeFormatter.current
//    Text(fmt.currency(balance))
//
//  It's driven by the `isArabic` flag in ForgeTheme:
//
//    CompositionLocalProvider(
//        LocalForgeFormatter provides ForgeFormatter(isArabic = rtl)
//    ) { ... }

val LocalForgeFormatter = staticCompositionLocalOf { ForgeFormatter(isArabic = false) }