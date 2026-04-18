package com.lukulent.finanzapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int,
    val isExpense: Boolean,
    val subject: String?,
    val date: LocalDate,
    val isDone: Boolean = false,
    val paymentMethod: PaymentMethod? = null,
    @ColumnInfo(defaultValue = "NULL")
    val amountRaw: String? = null
)
