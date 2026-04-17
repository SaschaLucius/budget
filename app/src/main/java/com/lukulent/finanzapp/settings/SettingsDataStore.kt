package com.lukulent.finanzapp.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val closeOnEntryKey = booleanPreferencesKey("close_on_entry")

    val closeOnEntry: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[closeOnEntryKey] ?: false
    }

    suspend fun setCloseOnEntry(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[closeOnEntryKey] = value
        }
    }
}
