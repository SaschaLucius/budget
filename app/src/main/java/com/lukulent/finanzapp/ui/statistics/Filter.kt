package com.lukulent.finanzapp.ui.statistics

import java.time.LocalDate
import java.time.YearMonth

sealed class Filter(val label: String) {
    data object ThisMonth : Filter("Dieser Monat")
    data object LastMonth : Filter("Letzter Monat")
    data object LastThreeMonths : Filter("Letzte 3 Monate")

    fun dateRange(): Pair<LocalDate, LocalDate> {
        val now = YearMonth.now()
        return when (this) {
            is ThisMonth -> {
                now.atDay(1) to now.atEndOfMonth()
            }
            is LastMonth -> {
                val last = now.minusMonths(1)
                last.atDay(1) to last.atEndOfMonth()
            }
            is LastThreeMonths -> {
                val threeAgo = now.minusMonths(2)
                threeAgo.atDay(1) to now.atEndOfMonth()
            }
        }
    }
}
