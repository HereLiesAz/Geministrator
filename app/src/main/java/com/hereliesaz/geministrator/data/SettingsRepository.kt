package com.hereliesaz.geministrator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferenceKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val THEME = stringPreferencesKey("theme")
    }

    val apiKey: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.API_KEY]
        }

    val theme: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.THEME]
        }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.API_KEY] = apiKey
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME] = theme
        }
    }
}
