package com.khadr.forge.features.budget.data

import com.khadr.forge.features.budget.data.BudgetCategory
import com.khadr.forge.features.budget.data.TransactionEntity
import com.khadr.forge.features.budget.data.TransactionType
import kotlin.math.roundToInt

// ══════════════════════════════════════════════════════════════════════════════
//  Forge Budget Allocation — Smart 50/30/20 Rule Engine
//
//  50% → Needs:   BILLS, HEALTH, FOOD, TRANSPORT
//  30% → Wants:   SHOPPING, OTHER, FREELANCE
//  20% → Savings: auto (income - needs - wants)
// ══════════════════════════════════════════════════════════════════════════════

// ─── Zone enum ────────────────────────────────────────────────────────────────
enum class BudgetZone(val defaultPercent: Int, val labelEn: String, val labelAr: String) {
    NEEDS  (50, "Needs",   "احتياجات"),
    WANTS  (30, "Wants",   "رغبات"),
    SAVINGS(20, "Savings", "مدخرات")
}

// ─── Category → Zone mapping ──────────────────────────────────────────────────
fun BudgetCategory.toZone(): BudgetZone = when (this) {
    BudgetCategory.FOOD,
    BudgetCategory.TRANSPORT,
    BudgetCategory.HEALTH,
    BudgetCategory.BILLS     -> BudgetZone.NEEDS

    BudgetCategory.SHOPPING,
    BudgetCategory.OTHER     -> BudgetZone.WANTS

    // Income categories — not expenses, won't hit zone logic
    BudgetCategory.SALARY,
    BudgetCategory.FREELANCE -> BudgetZone.SAVINGS
}

// ─── Zone split result ────────────────────────────────────────────────────────
data class ZoneSplit(
    val zone        : BudgetZone,
    val budget      : Double,    // allocated amount = income × percent / 100
    val spent       : Double,    // actual spend in this zone
    val percent     : Int,       // custom override (default = zone.defaultPercent)
    val usedPercent : Float,     // 0..1+ (> 1 = over budget)
) {
    val remaining : Double get() = budget - spent
    val isOverBudget: Boolean get() = spent > budget && spent > 0
}

// ─── Warning ──────────────────────────────────────────────────────────────────
enum class AllocationWarningLevel { INFO, WARNING, DANGER }

data class AllocationWarning(
    val level    : AllocationWarningLevel,
    val titleEn  : String,
    val titleAr  : String,
    val bodyEn   : String,
    val bodyAr   : String
)

// ─── Full allocation result ───────────────────────────────────────────────────
data class AllocationResult(
    val income       : Double,
    val totalSpent   : Double,
    val splits       : List<ZoneSplit>,
    val warnings     : List<AllocationWarning>,
    val savingsActual: Double,   // income - totalSpent (real savings)
    val spentPercent : Float,    // 0..1
) {
    val needsSplit   : ZoneSplit get() = splits.first { it.zone == BudgetZone.NEEDS }
    val wantsSplit   : ZoneSplit get() = splits.first { it.zone == BudgetZone.WANTS }
    val savingsSplit : ZoneSplit get() = splits.first { it.zone == BudgetZone.SAVINGS }
}

// ─── Custom allocation (user can override the percentages) ────────────────────
data class AllocationConfig(
    val needsPercent  : Int = 50,
    val wantsPercent  : Int = 30,
    val savingsPercent: Int = 20   // auto = 100 - needs - wants
) {
    val isValid: Boolean get() = needsPercent + wantsPercent + savingsPercent == 100
    val autoSavings: Int get() = 100 - needsPercent - wantsPercent
}

// ─── Engine ───────────────────────────────────────────────────────────────────
object AllocationEngine {

    fun compute(
        transactions: List<TransactionEntity>,
        income      : Double,
        config      : AllocationConfig = AllocationConfig()
    ): AllocationResult {
        if (income <= 0) return emptyResult()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalSpent = expenses.sumOf { it.amount }

        // Group by zone
        val spentByZone = mutableMapOf(
            BudgetZone.NEEDS   to 0.0,
            BudgetZone.WANTS   to 0.0,
            BudgetZone.SAVINGS to 0.0
        )
        expenses.forEach { tx ->
            val zone = tx.category.toZone()
            spentByZone[zone] = (spentByZone[zone] ?: 0.0) + tx.amount
        }

        // Build splits
        val splits = listOf(
            buildSplit(BudgetZone.NEEDS,   income, config.needsPercent,   spentByZone[BudgetZone.NEEDS]   ?: 0.0),
            buildSplit(BudgetZone.WANTS,   income, config.wantsPercent,   spentByZone[BudgetZone.WANTS]   ?: 0.0),
            buildSplit(BudgetZone.SAVINGS, income, config.savingsPercent, 0.0)   // savings = what's left
        )

        val savingsActual = income - totalSpent
        val warnings = generateWarnings(splits, income, totalSpent, savingsActual)

        return AllocationResult(
            income        = income,
            totalSpent    = totalSpent,
            splits        = splits,
            warnings      = warnings,
            savingsActual = savingsActual,
            spentPercent  = (totalSpent / income).toFloat().coerceIn(0f, 2f)
        )
    }

    private fun buildSplit(zone: BudgetZone, income: Double, percent: Int, spent: Double): ZoneSplit {
        val budget      = income * percent / 100.0
        val usedPercent = if (budget > 0) (spent / budget).toFloat() else 0f
        return ZoneSplit(
            zone        = zone,
            budget      = budget,
            spent       = spent,
            percent     = percent,
            usedPercent = usedPercent
        )
    }

    private fun generateWarnings(
        splits      : List<ZoneSplit>,
        income      : Double,
        totalSpent  : Double,
        savings     : Double
    ): List<AllocationWarning> {
        val warnings = mutableListOf<AllocationWarning>()

        splits.forEach { split ->
            val pct = (split.usedPercent * 100).roundToInt()
            when {
                split.isOverBudget -> warnings.add(
                    AllocationWarning(
                        level   = AllocationWarningLevel.DANGER,
                        titleEn = "${split.zone.labelEn} budget exceeded",
                        titleAr = "تجاوز ميزانية ${split.zone.labelAr}",
                        bodyEn  = "You've spent ${pct}% of your ${split.zone.labelEn.lowercase()} budget. Consider cutting back.",
                        bodyAr  = "صرفت $pct% من ميزانية ${split.zone.labelAr}. فكر في التقليل."
                    )
                )
                split.usedPercent >= 0.8f && !split.isOverBudget -> warnings.add(
                    AllocationWarning(
                        level   = AllocationWarningLevel.WARNING,
                        titleEn = "${split.zone.labelEn} nearing limit",
                        titleAr = "${split.zone.labelAr} تقترب من الحد",
                        bodyEn  = "You've used ${pct}% of your ${split.zone.labelEn.lowercase()} budget.",
                        bodyAr  = "استخدمت $pct% من ميزانية ${split.zone.labelAr}."
                    )
                )
            }
        }

        // Overall over-spending
        if (totalSpent > income) {
            warnings.add(0, AllocationWarning(
                level   = AllocationWarningLevel.DANGER,
                titleEn = "Spending exceeds income",
                titleAr = "المصروف يتجاوز الدخل",
                bodyEn  = "You've spent ${((totalSpent / income) * 100).roundToInt()}% of your income this month.",
                bodyAr  = "صرفت ${((totalSpent / income) * 100).roundToInt()}% من دخلك هذا الشهر."
            ))
        }

        // Negative savings
        if (savings < 0) {
            warnings.add(AllocationWarning(
                level   = AllocationWarningLevel.DANGER,
                titleEn = "No savings this month",
                titleAr = "لا مدخرات هذا الشهر",
                bodyEn  = "You're spending more than you earn. Review your expenses.",
                bodyAr  = "أنت تصرف أكثر مما تكسب. راجع مصاريفك."
            ))
        } else if (savings < income * 0.1) {
            warnings.add(AllocationWarning(
                level   = AllocationWarningLevel.WARNING,
                titleEn = "Low savings rate",
                titleAr = "معدل ادخار منخفض",
                bodyEn  = "Aim for at least 20% savings. You're currently at ${((savings / income) * 100).roundToInt()}%.",
                bodyAr  = "استهدف ادخار 20% على الأقل. أنت حالياً عند ${((savings / income) * 100).roundToInt()}%."
            ))
        }

        return warnings
    }

    private fun emptyResult() = AllocationResult(
        income = 0.0, totalSpent = 0.0,
        splits = BudgetZone.entries.map { ZoneSplit(it, 0.0, 0.0, it.defaultPercent, 0f) },
        warnings = emptyList(), savingsActual = 0.0, spentPercent = 0f
    )
}