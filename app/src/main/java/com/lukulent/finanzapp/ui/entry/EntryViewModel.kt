package com.lukulent.finanzapp.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukulent.finanzapp.data.model.PaymentMethod
import com.lukulent.finanzapp.data.model.Transaction
import com.lukulent.finanzapp.data.repository.TransactionRepository
import com.lukulent.finanzapp.settings.SettingsDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class EntryViewModel(
    private val repository: TransactionRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount

    private val _subject = MutableStateFlow("")
    val subject: StateFlow<String> = _subject

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date

    private val _amountError = MutableStateFlow<String?>(null)
    val amountError: StateFlow<String?> = _amountError

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone

    private val _isExpense = MutableStateFlow(false)
    val isExpense: StateFlow<Boolean> = _isExpense

    private val _editingId = MutableStateFlow<Long?>(null)
    val editingId: StateFlow<Long?> = _editingId

    private val _closeOnEntry = MutableStateFlow(false)
    val closeOnEntry: StateFlow<Boolean> = _closeOnEntry

    private val _showPaymentMethod = MutableStateFlow(false)
    val showPaymentMethod: StateFlow<Boolean> = _showPaymentMethod

    private val _paymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val paymentMethod: StateFlow<PaymentMethod?> = _paymentMethod

    private val _backgroundColor = MutableStateFlow(com.lukulent.finanzapp.settings.SettingsDataStore.DEFAULT_BACKGROUND_COLOR)
    val backgroundColor: StateFlow<Long> = _backgroundColor

    sealed class UiEvent {
        data object NavigateBack : UiEvent()
        data object CloseApp : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    init {
        viewModelScope.launch {
            settingsDataStore.closeOnEntry.collect { _closeOnEntry.value = it }
        }
        viewModelScope.launch {
            settingsDataStore.showPaymentMethod.collect { _showPaymentMethod.value = it }
        }
        viewModelScope.launch {
            settingsDataStore.backgroundColor.collect { _backgroundColor.value = it }
        }
    }

    fun load(id: Long) {
        viewModelScope.launch {
            val transaction = repository.getById(id) ?: return@launch
            _editingId.value = transaction.id
            _amount.value = transaction.amountRaw ?: transaction.amount.toString()
            _subject.value = transaction.subject ?: ""
            _date.value = transaction.date
            _isDone.value = transaction.isDone
            _isExpense.value = transaction.isExpense
            _paymentMethod.value = transaction.paymentMethod
        }
    }

    fun setAmount(value: String) {
        // Allow digits and at most one decimal separator (dot or comma)
        val filtered = value.filter { it.isDigit() || it == '.' || it == ',' }
        val normalized = filtered.replace(',', '.')
        val dotCount = normalized.count { it == '.' }
        _amount.value = if (dotCount <= 1) normalized else _amount.value
        _amountError.value = null
    }

    fun setSubject(value: String) {
        _subject.value = value
    }

    fun setDate(value: LocalDate) {
        _date.value = value
    }

    fun setIsDone(value: Boolean) {
        _isDone.value = value
    }

    fun setIsExpense(value: Boolean) {
        _isExpense.value = value
    }

    fun setPaymentMethod(method: PaymentMethod?) {
        _paymentMethod.value = method
    }

    fun delete() {
        val id = _editingId.value ?: return
        viewModelScope.launch {
            repository.getById(id)?.let { repository.delete(it) }
            _uiEvent.emit(UiEvent.NavigateBack)
        }
    }

    fun setCloseOnEntry(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCloseOnEntry(value)
        }
    }

    fun setShowPaymentMethod(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowPaymentMethod(value)
        }
    }

    fun setBackgroundColor(color: Long) {
        viewModelScope.launch {
            settingsDataStore.setBackgroundColor(color)
        }
    }

    fun save(isExpense: Boolean) {
        val raw = _amount.value
        val parsed = raw.replace(',', '.').toDoubleOrNull()
        if (raw.isBlank() || parsed == null || parsed <= 0) {
            _amountError.value = "Bitte einen gültigen Betrag eingeben"
            return
        }
        val amountInt = parsed.toInt()

        viewModelScope.launch {
            val transaction = Transaction(
                id = _editingId.value ?: 0,
                amount = amountInt,
                isExpense = isExpense,
                subject = _subject.value.ifBlank { null },
                date = _date.value,
                isDone = _isDone.value,
                paymentMethod = _paymentMethod.value,
                amountRaw = if (raw.contains('.') || raw.contains(',')) raw else null
            )
            if (_editingId.value != null) {
                repository.update(transaction)
                _uiEvent.emit(UiEvent.NavigateBack)
            } else {
                repository.insert(transaction)
                val close = settingsDataStore.closeOnEntry.first()
                if (close) {
                    _uiEvent.emit(UiEvent.CloseApp)
                } else {
                    _amount.value = ""
                    _subject.value = ""
                    _amountError.value = null
                    _paymentMethod.value = null
                }
            }
        }
    }

    class Factory(
        private val repository: TransactionRepository,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EntryViewModel(repository, settingsDataStore) as T
        }
    }
}
