package com.khadr.forge.features.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadr.forge.features.tasks.data.TaskEntity
import com.khadr.forge.features.tasks.data.TaskPriorityEntity
import com.khadr.forge.features.tasks.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Filter ───────────────────────────────────────────────────────────────────
enum class TaskFilter { ALL, PENDING, DONE }

// ─── UI State ─────────────────────────────────────────────────────────────────
data class TasksUiState(
    val tasks        : List<TaskEntity> = emptyList(),
    val filter       : TaskFilter       = TaskFilter.ALL,
    val isAddSheetOpen : Boolean        = false,
    val editingTask  : TaskEntity?      = null,
    val isLoading    : Boolean          = true
)

// ─── Form State ───────────────────────────────────────────────────────────────
data class TaskFormState(
    val title       : String              = "",
    val description : String              = "",
    val category    : String              = "",
    val priority    : TaskPriorityEntity  = TaskPriorityEntity.MEDIUM
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState   = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    // Raw full list; filtered in uiState based on current filter
    private val _allTasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    init {
        viewModelScope.launch {
            repository.getAllTasks().collect { tasks ->
                _allTasks.value = tasks
                applyFilter()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilter()
    }

    private fun applyFilter() {
        val all = _allTasks.value
        val filtered = when (_uiState.value.filter) {
            TaskFilter.ALL     -> all
            TaskFilter.PENDING -> all.filter { !it.isDone }
            TaskFilter.DONE    -> all.filter { it.isDone }
        }
        _uiState.update { it.copy(tasks = filtered) }
    }

    // ── Sheet ─────────────────────────────────────────────────────────────────
    fun openAddSheet() {
        _formState.value = TaskFormState()
        _uiState.update { it.copy(isAddSheetOpen = true, editingTask = null) }
    }

    fun openEditSheet(task: TaskEntity) {
        _formState.value = TaskFormState(
            title       = task.title,
            description = task.description,
            category    = task.category,
            priority    = task.priority
        )
        _uiState.update { it.copy(isAddSheetOpen = true, editingTask = task) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(isAddSheetOpen = false, editingTask = null) }
    }

    // ── Form handlers ─────────────────────────────────────────────────────────
    fun onTitleChange(v: String)                { _formState.update { it.copy(title = v) } }
    fun onDescChange(v: String)                 { _formState.update { it.copy(description = v) } }
    fun onCategoryChange(v: String)             { _formState.update { it.copy(category = v) } }
    fun onPriorityChange(v: TaskPriorityEntity) { _formState.update { it.copy(priority = v) } }

    // ── Validation event ─────────────────────────────────────────────────────
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()
    fun clearValidationError() { _validationError.value = null }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun saveTask() {
        val form = _formState.value
        if (form.title.isBlank()) {
            _validationError.value = "title_required"
            return
        }

        viewModelScope.launch {
            val editing = _uiState.value.editingTask
            if (editing != null) {
                repository.updateTask(
                    editing.copy(
                        title       = form.title.trim(),
                        description = form.description.trim(),
                        category    = form.category.trim(),
                        priority    = form.priority
                    )
                )
            } else {
                repository.insertTask(
                    TaskEntity(
                        title       = form.title.trim(),
                        description = form.description.trim(),
                        category    = form.category.trim(),
                        priority    = form.priority
                    )
                )
            }
            closeSheet()
        }
    }

    // ── Toggle done ───────────────────────────────────────────────────────────
    fun toggleTask(id: Long, currentDone: Boolean) {
        viewModelScope.launch { repository.toggleDone(id) }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.deleteTask(id) }
    }

    // ── Clear completed ───────────────────────────────────────────────────────
    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }
}