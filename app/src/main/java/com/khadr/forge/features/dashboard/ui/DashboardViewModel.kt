package com.khadr.forge.features.dashboard.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadr.forge.features.budget.data.BudgetRepository
import com.khadr.forge.features.reminders.data.ReminderRepository
import com.khadr.forge.features.schedule.data.EventEntity
import com.khadr.forge.features.schedule.data.ScheduleRepository
import com.khadr.forge.features.tasks.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

// ─── Dashboard data models ────────────────────────────────────────────────────
data class DashboardTask(
    val id       : Long,
    val title    : String,
    val category : String,
    val priority : String,
    val isDone   : Boolean
)

data class DashboardEvent(
    val id        : Long,
    val title     : String,
    val startTime : LocalTime,
    val endTime   : LocalTime?
)

data class DashboardReminder(
    val id        : Long,
    val title     : String,
    val timeLabel : String
)

data class BudgetSummary(
    val income  : Double,
    val spent   : Double,
    val balance : Double
)

// ─── UI State ─────────────────────────────────────────────────────────────────
data class DashboardUiState(
    val greeting         : String              = "",
    val tasksRemaining   : Int                 = 0,
    val budgetLeft       : Double              = 0.0,
    val eventsToday      : Int                 = 0,
    val topTask          : DashboardTask?      = null,
    val recentTasks      : List<DashboardTask> = emptyList(),
    val upcomingEvents   : List<DashboardEvent> = emptyList(),
    val budgetSummary    : BudgetSummary       = BudgetSummary(0.0, 0.0, 0.0),
    val upcomingReminders: List<DashboardReminder> = emptyList()
)

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository    : TaskRepository,
    private val scheduleRepository: ScheduleRepository,
    private val budgetRepository  : BudgetRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val today    = LocalDate.now()
        val monthFrom = YearMonth.now().atDay(1)
        val monthTo   = YearMonth.now().atEndOfMonth()

        // ── Greeting ──────────────────────────────────────────────────────────
        val hour = LocalTime.now().hour
        val greeting = when {
            hour < 12 -> "greeting_morning"
            hour < 17 -> "greeting_afternoon"
            else      -> "greeting_evening"
        }
        _uiState.update { it.copy(greeting = greeting) }

        // ── Tasks ─────────────────────────────────────────────────────────────
        viewModelScope.launch {
            taskRepository.getAllTasks().collect { tasks ->
                val pending = tasks.filter { !it.isDone }
                val topTask = pending.minByOrNull { it.priority.ordinal }?.let { entity ->
                    DashboardTask(
                        id       = entity.id,
                        title    = entity.title,
                        category = entity.category.ifBlank { entity.priority.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        priority = entity.priority.name,
                        isDone   = entity.isDone
                    )
                }
                val recent = tasks.take(5).map { entity ->
                    DashboardTask(
                        id = entity.id,
                        title = entity.title,
                        category = entity.category,
                        priority = entity.priority.name,
                        isDone = entity.isDone
                    )
                }
                _uiState.update { it.copy(tasksRemaining = pending.size, topTask = topTask, recentTasks = recent) }
            }
        }

        // ── Schedule ──────────────────────────────────────────────────────────
        viewModelScope.launch {
            scheduleRepository.getEventsForDate(today).collect { events ->
                val mapped = events.map { e ->
                    DashboardEvent(id = e.id, title = e.title, startTime = e.startTime, endTime = e.endTime)
                }
                _uiState.update { it.copy(eventsToday = events.size, upcomingEvents = mapped) }
            }
        }

        // ── Budget ────────────────────────────────────────────────────────────
        viewModelScope.launch {
            combine(
                budgetRepository.getTotalIncome(monthFrom, monthTo),
                budgetRepository.getTotalExpenses(monthFrom, monthTo)
            ) { income, expenses ->
                BudgetSummary(income = income, spent = expenses, balance = income - expenses)
            }.collect { summary ->
                _uiState.update { it.copy(budgetSummary = summary, budgetLeft = summary.balance) }
            }
        }

        // ── Reminders ─────────────────────────────────────────────────────────
        viewModelScope.launch {
            reminderRepository.getUpcomingReminders().collect { reminders ->
                val mapped = reminders.take(3).map { r ->
                    val timeLabel = buildTimeLabel(r.date, r.time, today)
                    DashboardReminder(id = r.id, title = r.title, timeLabel = timeLabel)
                }
                _uiState.update { it.copy(upcomingReminders = mapped) }
            }
        }
    }

    // ── Task toggle ───────────────────────────────────────────────────────────
    fun toggleTask(id: Long) {
        viewModelScope.launch {
            taskRepository.toggleDone(id)
        }
    }

    // ── Time label helper ─────────────────────────────────────────────────────
    private fun buildTimeLabel(date: LocalDate, time: LocalTime, today: LocalDate): String {
        val tomorrow = today.plusDays(1)
        val prefix = when (date) {
            today    -> "Today"
            tomorrow -> "Tomorrow"
            else     -> date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        }
        return "$prefix · ${time.hour.toString().padStart(2,'0')}:${time.minute.toString().padStart(2,'0')}"
    }
}