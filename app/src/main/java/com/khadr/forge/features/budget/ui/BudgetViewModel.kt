package com.khadr.forge.features.budget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadr.forge.features.budget.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class BudgetUiState(
    val currentMonth    : YearMonth              = YearMonth.now(),
    val totalIncome     : Double                 = 0.0,
    val totalExpenses   : Double                 = 0.0,
    val balance         : Double                 = 0.0,
    val transactions    : List<TransactionEntity> = emptyList(),
    val allocation      : AllocationResult?      = null,
    val allocationConfig: AllocationConfig       = AllocationConfig(),
    val showAllocation  : Boolean                = true,
    val isSheetOpen     : Boolean                = false,
    val isConfigOpen    : Boolean                = false,
    val editingTx       : TransactionEntity?     = null,
    val isLoading       : Boolean                = true
)

data class TransactionFormState(
    val title    : String          = "",
    val amount   : String          = "",
    val type     : TransactionType = TransactionType.EXPENSE,
    val category : BudgetCategory  = BudgetCategory.OTHER,
    val note     : String          = "",
    val date     : LocalDate       = LocalDate.now()
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _month     = MutableStateFlow(YearMonth.now())
    private val _config    = MutableStateFlow(AllocationConfig())
    private val _uiState   = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    // Validation toast event
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()
    fun clearValidationError() { _validationError.value = null }

    init {
        viewModelScope.launch {
            combine(_month, _config) { month, config -> Pair(month, config) }
                .collectLatest { (month, config) ->
                    val from = month.atDay(1)
                    val to   = month.atEndOfMonth()

                    combine(
                        repository.getTransactionsInRange(from, to),
                        repository.getTotalIncome(from, to),
                        repository.getTotalExpenses(from, to)
                    ) { txs, income, expenses ->
                        val allocation = AllocationEngine.compute(txs, income, config)
                        _uiState.update {
                            it.copy(
                                currentMonth     = month,
                                transactions     = txs,
                                totalIncome      = income,
                                totalExpenses    = expenses,
                                balance          = income - expenses,
                                allocation       = allocation,
                                allocationConfig = config,
                                isLoading        = false
                            )
                        }
                    }.collect()
                }
        }
    }

    // ── Month navigation ──────────────────────────────────────────────────────
    fun previousMonth() { _month.update { it.minusMonths(1) } }
    fun nextMonth()     { _month.update { it.plusMonths(1) } }

    // ── Allocation config ─────────────────────────────────────────────────────
    fun setAllocationConfig(config: AllocationConfig) {
        _config.value = config
        _uiState.update { it.copy(allocationConfig = config) }
    }

    fun toggleAllocation() { _uiState.update { it.copy(showAllocation = !it.showAllocation) } }
    fun openConfigSheet()  { _uiState.update { it.copy(isConfigOpen = true) } }
    fun closeConfigSheet() { _uiState.update { it.copy(isConfigOpen = false) } }

    // ── Transaction sheet ─────────────────────────────────────────────────────
    fun openAddSheet() {
        _formState.value = TransactionFormState()
        _uiState.update { it.copy(isSheetOpen = true, editingTx = null) }
    }

    fun openEditSheet(tx: TransactionEntity) {
        _formState.value = TransactionFormState(
            title    = tx.title,
            amount   = tx.amount.toBigDecimal().stripTrailingZeros().toPlainString(),
            type     = tx.type,
            category = tx.category,
            note     = tx.note,
            date     = tx.date
        )
        _uiState.update { it.copy(isSheetOpen = true, editingTx = tx) }
    }

    fun closeSheet() { _uiState.update { it.copy(isSheetOpen = false, editingTx = null) } }

    // ── Form ──────────────────────────────────────────────────────────────────
    fun onTitleChange(v: String)            { _formState.update { it.copy(title = v) } }
    fun onAmountChange(v: String)           { _formState.update { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) } }
    fun onTypeChange(v: TransactionType)    { _formState.update { it.copy(type = v) } }
    fun onCategoryChange(v: BudgetCategory) { _formState.update { it.copy(category = v) } }
    fun onNoteChange(v: String)             { _formState.update { it.copy(note = v) } }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun saveTransaction() {
        val form   = _formState.value
        val amount = form.amount.toDoubleOrNull()

        when {
            amount == null -> { _validationError.value = "amount_required"; return }
            amount <= 0    -> { _validationError.value = "amount_invalid";  return }
        }

        viewModelScope.launch {
            val editing = _uiState.value.editingTx
            if (editing != null) {
                repository.updateTransaction(
                    editing.copy(title = form.title.trim(), amount = amount!!, type = form.type,
                        category = form.category, note = form.note.trim())
                )
            } else {
                repository.addTransaction(
                    TransactionEntity(title = form.title.trim(), amount = amount!!, type = form.type,
                        category = form.category, note = form.note.trim(), date = form.date)
                )
            }
            closeSheet()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }
}