package com.khadr.forge.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "forge_prefs")

object PrefKeys {
    val LANGUAGE     = stringPreferencesKey("language")
    val DARK_MODE    = booleanPreferencesKey("dark_mode")
    val FONT_CHOICE  = stringPreferencesKey("font_choice")
    val TEXT_SIZE    = floatPreferencesKey("text_size")
    val NOTIF_MODE   = stringPreferencesKey("notif_mode")
    val RINGTONE_URI = stringPreferencesKey("ringtone_uri")
}

enum class AppLanguage(val code: String) {
    ENGLISH("en"), ARABIC("ar");
    val isArabic get() = this == ARABIC
    companion object { fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: ENGLISH }
}

enum class NotifMode {
    SILENT, NOTIFICATION_ONLY, SOUND, FULL_ALARM;
    companion object { fun fromName(n: String?) = entries.firstOrNull { it.name == n } ?: FULL_ALARM }
}

enum class TextSizeOption(val scale: Float, val labelEn: String, val labelAr: String) {
    SMALL  (0.85f, "Small",   "صغير"),
    NORMAL (1.00f, "Normal",  "عادي"),
    LARGE  (1.15f, "Large",   "كبير"),
    X_LARGE(1.30f, "X-Large", "أكبر");
    companion object {
        fun fromScale(v: Float?) = entries.minByOrNull { kotlin.math.abs(it.scale - (v ?: 1f)) } ?: NORMAL
    }
}

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    val language: Flow<AppLanguage>   = context.dataStore.data.map { AppLanguage.fromCode(it[PrefKeys.LANGUAGE]) }
    val darkMode: Flow<Boolean?>      = context.dataStore.data.map { it[PrefKeys.DARK_MODE] }
    val textSize: Flow<TextSizeOption> = context.dataStore.data.map { TextSizeOption.fromScale(it[PrefKeys.TEXT_SIZE]) }
    val notifMode: Flow<NotifMode>    = context.dataStore.data.map { NotifMode.fromName(it[PrefKeys.NOTIF_MODE]) }
    val ringtoneUri: Flow<String>     = context.dataStore.data.map { it[PrefKeys.RINGTONE_URI] ?: "" }
    val fontChoice: Flow<com.khadr.forge.ui.theme.AppFont> = context.dataStore.data.map {
        com.khadr.forge.ui.theme.AppFont.entries.firstOrNull { f -> f.name == it[PrefKeys.FONT_CHOICE] }
            ?: com.khadr.forge.ui.theme.AppFont.SYSTEM
    }

    suspend fun setLanguage(v: AppLanguage)                          { context.dataStore.edit { it[PrefKeys.LANGUAGE]     = v.code } }
    suspend fun setDarkMode(v: Boolean)                              { context.dataStore.edit { it[PrefKeys.DARK_MODE]    = v } }
    suspend fun setFontChoice(v: com.khadr.forge.ui.theme.AppFont)   { context.dataStore.edit { it[PrefKeys.FONT_CHOICE]  = v.name } }
    suspend fun setTextSize(v: TextSizeOption)                       { context.dataStore.edit { it[PrefKeys.TEXT_SIZE]    = v.scale } }
    suspend fun setNotifMode(v: NotifMode)                           { context.dataStore.edit { it[PrefKeys.NOTIF_MODE]   = v.name } }
    suspend fun setRingtoneUri(v: String)                            { context.dataStore.edit { it[PrefKeys.RINGTONE_URI] = v } }
}