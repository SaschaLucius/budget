package com.lukulent.finanzapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lukulent.finanzapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query(
        "SELECT * FROM transactions WHERE date >= :from AND date <= :to ORDER BY date DESC, id DESC"
    )
    fun queryByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>>

    @Query("UPDATE transactions SET isDone = 1 WHERE date >= :from AND date <= :to")
    suspend fun markAllDoneInRange(from: LocalDate, to: LocalDate)

    @Query("UPDATE transactions SET isDone = 1 WHERE id IN (:ids)")
    suspend fun markDoneByIds(ids: List<Long>)
}
