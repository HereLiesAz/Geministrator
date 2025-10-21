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

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    companion object {
        private val API_KEY = stringPreferencesKey("jules_api_key")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val THEME = stringPreferencesKey("theme")
        private val GCP_PROJECT_ID = stringPreferencesKey("gcp_project_id")
        private val GCP_LOCATION = stringPreferencesKey("gcp_location")
        private val GEMINI_MODEL_NAME = stringPreferencesKey("gemini_model_name")
        private val ENABLED_ROLES = stringSetPreferencesKey("enabled_roles")
    }

    override val apiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[API_KEY]
    }

    override val geminiApiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY]
    }

    override val theme: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[THEME]
    }

    override val gcpProjectId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GCP_PROJECT_ID]
    }

    override val gcpLocation: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GCP_LOCATION]
    }

    override val geminiModelName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_MODEL_NAME]
    }

    override val enabledRoles: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[ENABLED_ROLES] ?: emptySet()
    }

    override suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { settings ->
            settings[API_KEY] = apiKey
        }
    }

    override suspend fun saveGeminiApiKey(geminiApiKey: String) {
        context.dataStore.edit { settings ->
            settings[GEMINI_API_KEY] = geminiApiKey
        }
    }

    override suspend fun saveTheme(theme: String) {
        context.dataStore.edit { settings ->
            settings[THEME] = theme
        }
    }

    override suspend fun saveGcpProjectId(gcpProjectId: String) {
        context.dataStore.edit { settings ->
            settings[GCP_PROJECT_ID] = gcpProjectId
        }
    }

    override suspend fun saveGcpLocation(gcpLocation: String) {
        context.dataStore.edit { settings ->
            settings[GCP_LOCATION] = gcpLocation
        }
    }

    override suspend fun saveGeminiModelName(geminiModelName: String) {
        context.dataStore.edit { settings ->
            settings[GEMINI_MODEL_NAME] = geminiModelName
        }
    }

    override suspend fun saveEnabledRoles(enabledRoles: Set<String>) {
        context.dataStore.edit { settings ->
            settings[ENABLED_ROLES] = enabledRoles
        }
    }
}
