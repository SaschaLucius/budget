package com.lukulent.finanzapp.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukulent.finanzapp.data.model.PaymentMethod
import com.lukulent.finanzapp.data.model.Transaction
import com.lukulent.finanzapp.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

class StatisticsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<Filter>(Filter.ThisMonth)
    val selectedFilter: StateFlow<Filter> = _selectedFilter

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    // Balance counts only undone transactions
    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance

    // Selected months for bulk done marking
    private val _selectedMonths = MutableStateFlow<Set<YearMonth>>(emptySet())
    val selectedMonths: StateFlow<Set<YearMonth>> = _selectedMonths

    private val _selectedMonthsBalance = MutableStateFlow(0)
    val selectedMonthsBalance: StateFlow<Int> = _selectedMonthsBalance

    private val _paymentFilter = MutableStateFlow<PaymentMethod?>(null)
    val paymentFilter: StateFlow<PaymentMethod?> = _paymentFilter

    private var queryJob: Job? = null

    init {
        applyFilter(Filter.ThisMonth)
    }

    fun setFilter(filter: Filter) {
        _selectedFilter.value = filter
        _selectedMonths.value = emptySet()
        applyFilter(filter)
    }

    fun setPaymentFilter(method: PaymentMethod?) {
        _paymentFilter.value = method
        recalcSelectedBalance(_transactions.value, _selectedMonths.value)
    }

    private fun applyFilter(filter: Filter) {
        queryJob?.cancel()
        val (from, to) = filter.dateRange()
        queryJob = viewModelScope.launch {
            repository.queryByDateRange(from, to).collect { list ->
                _transactions.value = list
                _balance.value = list
                    .filter { !it.isDone }
                    .sumOf { if (it.isExpense) -it.amount else it.amount }
                recalcSelectedBalance(list, _selectedMonths.value)
            }
        }
    }

    fun toggleMonthSelection(yearMonth: YearMonth) {
        val current = _selectedMonths.value
        _selectedMonths.value = if (yearMonth in current) current - yearMonth else current + yearMonth
        recalcSelectedBalance(_transactions.value, _selectedMonths.value)
    }

    fun clearSelection() {
        _selectedMonths.value = emptySet()
        _selectedMonthsBalance.value = 0
    }

    fun selectAllPending(months: Set<YearMonth>) {
        _selectedMonths.value = months
        recalcSelectedBalance(_transactions.value, months)
    }

    private fun recalcSelectedBalance(allTransactions: List<Transaction>, months: Set<YearMonth>) {
        val filter = _paymentFilter.value
        _selectedMonthsBalance.value = allTransactions
            .filter { !it.isDone && YearMonth.from(it.date) in months && (filter == null || it.paymentMethod == filter) }
            .sumOf { if (it.isExpense) -it.amount else it.amount }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun markMonthDone(yearMonth: YearMonth) {
        viewModelScope.launch {
            repository.markAllDoneInRange(
                from = yearMonth.atDay(1),
                to = yearMonth.atEndOfMonth()
            )
        }
    }

    fun markSelectedMonthsDone() {
        val months = _selectedMonths.value
        val filter = _paymentFilter.value
        viewModelScope.launch {
            if (filter == null) {
                // No payment filter: use fast SQL range update
                months.forEach { ym ->
                    repository.markAllDoneInRange(
                        from = ym.atDay(1),
                        to = ym.atEndOfMonth()
                    )
                }
            } else {
                // Payment filter active: only mark the visible transactions
                val ids = _transactions.value
                    .filter { !it.isDone && YearMonth.from(it.date) in months && it.paymentMethod == filter }
                    .map { it.id }
                if (ids.isNotEmpty()) repository.markDoneByIds(ids)
            }
            _selectedMonths.value = emptySet()
        }
    }

    class Factory(
        private val repository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(repository) as T
        }
    }
}
