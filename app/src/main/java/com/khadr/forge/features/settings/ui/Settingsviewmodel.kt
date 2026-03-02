package com.khadr.forge.features.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadr.forge.core.preferences.AppPreferences
import com.khadr.forge.core.preferences.NotifMode
import com.khadr.forge.core.preferences.TextSizeOption
import com.khadr.forge.ui.theme.AppFont
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode    : Boolean?       = null,
    val fontChoice  : AppFont        = AppFont.SYSTEM,
    val textSize    : TextSizeOption = TextSizeOption.NORMAL,
    val notifMode   : NotifMode      = NotifMode.FULL_ALARM,
    val ringtoneUri : String         = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.darkMode,
        prefs.fontChoice,
        prefs.textSize,
        prefs.notifMode,
        prefs.ringtoneUri
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            darkMode    = values[0] as Boolean?,
            fontChoice  = values[1] as AppFont,
            textSize    = values[2] as TextSizeOption,
            notifMode   = values[3] as NotifMode,
            ringtoneUri = values[4] as String
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setDarkMode(v: Boolean)      { viewModelScope.launch { prefs.setDarkMode(v) } }
    fun setFontChoice(v: AppFont)    { viewModelScope.launch { prefs.setFontChoice(v) } }
    fun setTextSize(v: TextSizeOption){ viewModelScope.launch { prefs.setTextSize(v) } }
    fun setNotifMode(v: NotifMode)   { viewModelScope.launch { prefs.setNotifMode(v) } }
    fun setRingtoneUri(v: String)    { viewModelScope.launch { prefs.setRingtoneUri(v) } }
}