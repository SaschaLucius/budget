package com.lukulent.finanzapp.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val closeOnEntryKey = booleanPreferencesKey("close_on_entry")
    private val showPaymentMethodKey = booleanPreferencesKey("show_payment_method")
    private val backgroundColorKey = longPreferencesKey("background_color")

    companion object {
        const val DEFAULT_BACKGROUND_COLOR = 0xFFFFFBFEL
    }

    val closeOnEntry: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[closeOnEntryKey] ?: false
    }

    val showPaymentMethod: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[showPaymentMethodKey] ?: false
    }

    val backgroundColor: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[backgroundColorKey] ?: DEFAULT_BACKGROUND_COLOR
    }

    suspend fun setCloseOnEntry(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[closeOnEntryKey] = value
        }
    }

    suspend fun setShowPaymentMethod(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[showPaymentMethodKey] = value
        }
    }

    suspend fun setBackgroundColor(color: Long) {
        context.dataStore.edit { prefs ->
            prefs[backgroundColorKey] = color
        }
    }
}
