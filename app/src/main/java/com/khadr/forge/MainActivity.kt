package com.khadr.forge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.khadr.forge.core.preferences.AppPreferences
import com.khadr.forge.core.preferences.TextSizeOption
import com.khadr.forge.core.ui.ForgeBottomBar
import com.khadr.forge.core.ui.ForgeNavHost
import com.khadr.forge.core.ui.ForgeToastHost
import com.khadr.forge.core.ui.LocalToastState
import com.khadr.forge.core.ui.rememberToastState
import com.khadr.forge.ui.theme.AppFont
import com.khadr.forge.ui.theme.ForgeTheme
import com.khadr.forge.ui.theme.buildFontConfig
import com.khadr.forge.ui.theme.neumorphic
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val config = LocalConfiguration.current
            val systemLocale: Locale = remember(config) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                    config.locales.get(0) ?: Locale.getDefault()
                else {
                    @Suppress("DEPRECATION")
                    config.locale ?: Locale.getDefault()
                }
            }
            val isArabic   = remember(systemLocale) { systemLocale.language == "ar" }
            val darkPref   by prefs.darkMode.collectAsState(initial = null)
            val fontChoice by prefs.fontChoice.collectAsState(initial = AppFont.SYSTEM)
            val textSize   by prefs.textSize.collectAsState(initial = TextSizeOption.NORMAL)
            val isDark      = darkPref ?: isSystemInDarkTheme()
            val fontConfig  = remember(fontChoice, isArabic) { buildFontConfig(fontChoice, isArabic) }
            val toastState  = rememberToastState()

            ForgeTheme(
                darkTheme  = isDark,
                rtl        = isArabic,
                fontConfig = fontConfig,
                textScale  = textSize.scale,
                toastState = toastState
            ) {
                ForgeApp()
            }
        }
    }
}

@Composable
fun ForgeApp() {
    val navController = rememberNavController()
    val neum          = MaterialTheme.neumorphic
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier       = Modifier.fillMaxSize(),
            containerColor = neum.background,
            bottomBar      = { ForgeBottomBar(navController) }
        ) { padding ->
            ForgeNavHost(navController = navController, modifier = Modifier.padding(padding))
        }
        ForgeToastHost()
    }
}