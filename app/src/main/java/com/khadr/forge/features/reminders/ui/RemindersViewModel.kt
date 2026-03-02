package com.khadr.forge.features.reminders.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadr.forge.R
import com.khadr.forge.core.alarm.AlarmScheduler
import com.khadr.forge.core.preferences.AppPreferences
import com.khadr.forge.core.preferences.NotifMode
import com.khadr.forge.features.reminders.data.ReminderEntity
import com.khadr.forge.features.reminders.data.ReminderRepository
import com.khadr.forge.features.reminders.data.RepeatInterval
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class ReminderTab { UPCOMING, PAST }

data class RemindersUiState(
    val tab         : ReminderTab          = ReminderTab.UPCOMING,
    val upcoming    : List<ReminderEntity> = emptyList(),
    val past        : List<ReminderEntity> = emptyList(),
    val isSheetOpen : Boolean              = false,
    val editingItem : ReminderEntity?      = null,
    val isLoading   : Boolean              = true
)

data class ReminderFormState(
    val title  : String         = "",
    val note   : String         = "",
    val date   : LocalDate      = LocalDate.now(),
    val time   : LocalTime      = LocalTime.of(9, 0),
    val repeat : RepeatInterval = RepeatInterval.NONE
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository  : ReminderRepository,
    private val prefs       : AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState   = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ReminderFormState())
    val formState: StateFlow<ReminderFormState> = _formState.asStateFlow()

    // Validation event consumed by UI → show toast
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()
    fun clearValidationError() { _validationError.value = null }

    init {
        viewModelScope.launch { repository.getUpcomingReminders().collect { items -> _uiState.update { it.copy(upcoming = items, isLoading = false) } } }
        viewModelScope.launch { repository.getPastReminders().collect   { items -> _uiState.update { it.copy(past = items) } } }
    }

    fun selectTab(tab: ReminderTab)         { _uiState.update { it.copy(tab = tab) } }
    fun openAddSheet()                       { _formState.value = ReminderFormState(); _uiState.update { it.copy(isSheetOpen = true, editingItem = null) } }
    fun openEditSheet(item: ReminderEntity)  { _formState.value = ReminderFormState(title = item.title, note = item.note, date = item.date, time = item.time, repeat = item.repeat); _uiState.update { it.copy(isSheetOpen = true, editingItem = item) } }
    fun closeSheet()                         { _uiState.update { it.copy(isSheetOpen = false, editingItem = null) } }
    fun onTitleChange(v: String)             { _formState.update { it.copy(title = v) } }
    fun onNoteChange(v: String)              { _formState.update { it.copy(note = v) } }
    fun onDateChange(v: LocalDate)           { _formState.update { it.copy(date = v) } }
    fun onTimeChange(v: LocalTime)           { _formState.update { it.copy(time = v) } }
    fun onRepeatChange(v: RepeatInterval)    { _formState.update { it.copy(repeat = v) } }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun saveReminder() {
        val form = _formState.value
        if (form.title.isBlank()) {
            _validationError.value = "title_required"
            return
        }
        viewModelScope.launch {
            val mode        = prefs.notifMode.first()
            val ringtoneUri = prefs.ringtoneUri.first()
            val editing     = _uiState.value.editingItem
            val id: Long

            if (editing != null) {
                val updated = editing.copy(title = form.title.trim(), note = form.note.trim(),
                    date = form.date, time = form.time, repeat = form.repeat, isDone = false)
                repository.updateReminder(updated)
                id = editing.id
                cancelAlarm(id)
                scheduleByMode(mode, id, updated.triggerAtMillis(), updated.title, updated.note, ringtoneUri)
            } else {
                val entity = ReminderEntity(title = form.title.trim(), note = form.note.trim(),
                    date = form.date, time = form.time, repeat = form.repeat)
                id = repository.addReminder(entity)
                scheduleByMode(mode, id, entity.triggerAtMillis(), entity.title, entity.note, ringtoneUri)
            }
            closeSheet()
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch { cancelAlarm(id); repository.deleteReminder(id) }
    }

    fun toggleDone(item: ReminderEntity) {
        viewModelScope.launch {
            val newDone = !item.isDone
            repository.setDone(item.id, newDone)
            if (newDone) cancelAlarm(item.id)
            else {
                val mode = prefs.notifMode.first()
                val uri  = prefs.ringtoneUri.first()
                scheduleByMode(mode, item.id, item.triggerAtMillis(), item.title, item.note, uri)
            }
        }
    }

    // ── Alarm dispatch by mode ────────────────────────────────────────────────
    private fun scheduleByMode(
        mode       : NotifMode,
        id         : Long,
        triggerMs  : Long,
        title      : String,
        note       : String,
        ringtoneUri: String
    ) {
        when (mode) {
            NotifMode.SILENT -> {
                // Schedule via AlarmManager but AlarmService will suppress sound+vibration
                AlarmScheduler.schedule(context, id, triggerMs, title, note,
                    silent = true, ringtoneUri = "")
            }
            NotifMode.NOTIFICATION_ONLY -> {
                AlarmScheduler.schedule(context, id, triggerMs, title, note,
                    silent = false, ringtoneUri = "", vibrationOnly = true)
            }
            NotifMode.SOUND -> {
                AlarmScheduler.schedule(context, id, triggerMs, title, note,
                    silent = false, ringtoneUri = ringtoneUri.ifBlank { "" }, vibrationOnly = false)
            }
            NotifMode.FULL_ALARM -> {
                AlarmScheduler.schedule(context, id, triggerMs, title, note,
                    silent = false, ringtoneUri = ringtoneUri.ifBlank { "" }, vibrationOnly = false,
                    fullScreen = true)
            }
        }
    }

    private fun cancelAlarm(id: Long) {
        AlarmScheduler.cancel(context, id)
    }
}