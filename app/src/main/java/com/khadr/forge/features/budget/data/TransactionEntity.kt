package com.khadr.forge.features.budget.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// ─── Transaction Type ─────────────────────────────────────────────────────────
enum class TransactionType { INCOME, EXPENSE }

// ─── Budget Category ──────────────────────────────────────────────────────────
enum class BudgetCategory {
    FOOD, TRANSPORT, SHOPPING, HEALTH, BILLS,
    SALARY, FREELANCE, OTHER
}

// ─── Transaction Entity ───────────────────────────────────────────────────────
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id        : Long               = 0,
    val title     : String,
    val amount    : Double,
    val type      : TransactionType    = TransactionType.EXPENSE,
    val category  : BudgetCategory     = BudgetCategory.OTHER,
    val date      : LocalDate          = LocalDate.now(),
    val note      : String             = "",
    val createdAt : Long               = System.currentTimeMillis()
)