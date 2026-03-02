package com.khadr.forge.features.budget.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        dao.getAllTransactions()

    fun getTransactionsInRange(from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>> =
        dao.getTransactionsInRange(from.toString(), to.toString())

    fun getTotalIncome(from: LocalDate, to: LocalDate): Flow<Double> =
        dao.getTotalIncome(from.toString(), to.toString())

    fun getTotalExpenses(from: LocalDate, to: LocalDate): Flow<Double> =
        dao.getTotalExpenses(from.toString(), to.toString())

    suspend fun addTransaction(t: TransactionEntity): Long    = dao.insertTransaction(t)
    suspend fun updateTransaction(t: TransactionEntity)       = dao.updateTransaction(t)
    suspend fun deleteTransaction(id: Long)                   = dao.deleteTransaction(id)
}