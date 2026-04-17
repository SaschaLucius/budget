package com.lukulent.finanzapp.data.repository

import com.lukulent.finanzapp.data.db.TransactionDao
import com.lukulent.finanzapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TransactionRepository(private val dao: TransactionDao) {
    suspend fun insert(transaction: Transaction) = dao.insert(transaction)
    suspend fun update(transaction: Transaction) = dao.update(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
    suspend fun getById(id: Long): Transaction? = dao.getById(id)
    fun queryByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        dao.queryByDateRange(from, to)

    suspend fun markAllDoneInRange(from: LocalDate, to: LocalDate) =
        dao.markAllDoneInRange(from, to)

    suspend fun markDoneByIds(ids: List<Long>) =
        dao.markDoneByIds(ids)
}
