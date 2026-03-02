package com.khadr.forge.features.schedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadr.forge.features.schedule.data.EventEntity
import com.khadr.forge.features.schedule.data.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

// ─── UI State ─────────────────────────────────────────────────────────────────
data class ScheduleUiState(
    val selectedDate    : LocalDate         = LocalDate.now(),
    val eventsToday     : List<EventEntity> = emptyList(),
    val datesWithEvents : Set<LocalDate>    = emptySet(),
    val isSheetOpen     : Boolean           = false,
    val editingEvent    : EventEntity?      = null,
    val isLoading       : Boolean           = true
)

// ─── Form State ───────────────────────────────────────────────────────────────
data class EventFormState(
    val title     : String      = "",
    val note      : String      = "",
    val startTime : LocalTime   = LocalTime.of(9, 0),
    val endTime   : LocalTime?  = null,
    val hasEndTime: Boolean     = false
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _uiState   = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(EventFormState())
    val formState: StateFlow<EventFormState> = _formState.asStateFlow()

    init {
        // Observe dates that have events (for calendar dots)
        viewModelScope.launch {
            repository.getDatesWithEvents().collect { dates ->
                _uiState.update { it.copy(datesWithEvents = dates.toSet()) }
            }
        }
        // Observe events for selected date
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date -> repository.getEventsForDate(date) }
                .collect { events ->
                    _uiState.update { it.copy(eventsToday = events, isLoading = false) }
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, isLoading = true) }
    }

    // ── Sheet ─────────────────────────────────────────────────────────────────
    fun openAddSheet() {
        _formState.value = EventFormState()
        _uiState.update { it.copy(isSheetOpen = true, editingEvent = null) }
    }

    fun openEditSheet(event: EventEntity) {
        _formState.value = EventFormState(
            title      = event.title,
            note       = event.note,
            startTime  = event.startTime,
            endTime    = event.endTime,
            hasEndTime = event.endTime != null
        )
        _uiState.update { it.copy(isSheetOpen = true, editingEvent = event) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(isSheetOpen = false, editingEvent = null) }
    }

    // ── Form ──────────────────────────────────────────────────────────────────
    fun onTitleChange(v: String)     { _formState.update { it.copy(title = v) } }
    fun onNoteChange(v: String)      { _formState.update { it.copy(note = v) } }
    fun onStartTimeChange(v: LocalTime)    { _formState.update { it.copy(startTime = v) } }
    fun onEndTimeChange(v: LocalTime?)     { _formState.update { it.copy(endTime = v) } }
    fun onToggleEndTime(enabled: Boolean)  {
        _formState.update { it.copy(hasEndTime = enabled, endTime = if (!enabled) null else it.endTime) }
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun saveEvent() {
        val form = _formState.value
        if (form.title.isBlank()) return
        viewModelScope.launch {
            val editing = _uiState.value.editingEvent
            val date    = _uiState.value.selectedDate
            if (editing != null) {
                repository.updateEvent(
                    editing.copy(
                        title     = form.title.trim(),
                        note      = form.note.trim(),
                        startTime = form.startTime,
                        endTime   = if (form.hasEndTime) form.endTime else null
                    )
                )
            } else {
                repository.addEvent(
                    EventEntity(
                        title     = form.title.trim(),
                        note      = form.note.trim(),
                        date      = date,
                        startTime = form.startTime,
                        endTime   = if (form.hasEndTime) form.endTime else null
                    )
                )
            }
            closeSheet()
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch { repository.deleteEvent(id) }
    }
}