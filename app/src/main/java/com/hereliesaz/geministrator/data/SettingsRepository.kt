package com.hereliesaz.geministrator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferenceKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val THEME = stringPreferencesKey("theme")
        val GCP_PROJECT_ID = stringPreferencesKey("gcp_project_id")
        val GCP_LOCATION = stringPreferencesKey("gcp_location")
        val GEMINI_MODEL_NAME = stringPreferencesKey("gemini_model_name")
        val ENABLED_ROLES = stringSetPreferencesKey("enabled_roles")
    }

    val enabledRoles: Flow<Set<String>>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.ENABLED_ROLES] ?: emptySet()
        }

    val apiKey: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.API_KEY]
        }

    val geminiApiKey: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.GEMINI_API_KEY]
        }

    val theme: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.THEME]
        }

    val gcpProjectId: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.GCP_PROJECT_ID]
        }

    val gcpLocation: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.GCP_LOCATION]
        }

    val geminiModelName: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.GEMINI_MODEL_NAME]
        }


    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.API_KEY] = apiKey
        }
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.GEMINI_API_KEY] = apiKey
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME] = theme
        }
    }

    suspend fun saveGcpProjectId(projectId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.GCP_PROJECT_ID] = projectId
        }
    }

    suspend fun saveGcpLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.GCP_LOCATION] = location
        }
    }

    suspend fun saveGeminiModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.GEMINI_MODEL_NAME] = modelName
        }
    }

    suspend fun saveEnabledRoles(enabledRoles: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.ENABLED_ROLES] = enabledRoles
        }
    }
}
