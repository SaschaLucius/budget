package com.lukulent.finanzapp.data.db

import androidx.room.TypeConverter
import com.lukulent.finanzapp.data.model.PaymentMethod
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod?): String? = method?.name

    @TypeConverter
    fun toPaymentMethod(name: String?): PaymentMethod? = name?.let { PaymentMethod.valueOf(it) }
}
