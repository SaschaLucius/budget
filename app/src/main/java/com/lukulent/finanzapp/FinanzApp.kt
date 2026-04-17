package com.lukulent.finanzapp

import android.app.Application
import com.lukulent.finanzapp.data.db.AppDatabase
import com.lukulent.finanzapp.data.repository.TransactionRepository
import com.lukulent.finanzapp.settings.SettingsDataStore

class FinanzApp : Application() {
    lateinit var repository: TransactionRepository
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = TransactionRepository(db.transactionDao())
        settingsDataStore = SettingsDataStore(this)
    }
}
