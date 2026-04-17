package com.lukulent.finanzapp.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukulent.finanzapp.data.model.Transaction
import com.lukulent.finanzapp.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<Filter>(Filter.ThisMonth)
    val selectedFilter: StateFlow<Filter> = _selectedFilter

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance

    private var queryJob: Job? = null

    init {
        applyFilter(Filter.ThisMonth)
    }

    fun setFilter(filter: Filter) {
        _selectedFilter.value = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: Filter) {
        queryJob?.cancel()
        val (from, to) = filter.dateRange()
        queryJob = viewModelScope.launch {
            repository.queryByDateRange(from, to).collect { list ->
                _transactions.value = list
                _balance.value = list.sumOf { if (it.isExpense) -it.amount else it.amount }
            }
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
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
