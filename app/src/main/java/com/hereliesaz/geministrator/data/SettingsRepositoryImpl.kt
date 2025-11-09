package com.hereliesaz.geministrator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("jules_api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val THEME = stringPreferencesKey("theme")
        val GCP_PROJECT_ID = stringPreferencesKey("gcp_project_id")
        val GCP_LOCATION = stringPreferencesKey("gcp_location")
        val GEMINI_MODEL_NAME = stringPreferencesKey("gemini_model_name")
        val ENABLED_ROLES = stringSetPreferencesKey("enabled_roles")
        val GITHUB_ACCESS_TOKEN = stringPreferencesKey("github_access_token")
    }

    override val apiKey: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.API_KEY] }
    override val geminiApiKey: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.GEMINI_API_KEY] }
    override val theme: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.THEME] }
    override val gcpProjectId: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.GCP_PROJECT_ID] }
    override val gcpLocation: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.GCP_LOCATION] }
    override val geminiModelName: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.GEMINI_MODEL_NAME] }
    override val enabledRoles: Flow<Set<String>> = context.dataStore.data.map { it[PreferencesKeys.ENABLED_ROLES] ?: emptySet() }
    override val githubAccessToken: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.GITHUB_ACCESS_TOKEN] }


    override suspend fun getApiKey(): String? = apiKey.first()

    override suspend fun getGeminiApiKey(): String? = geminiApiKey.first()

    override suspend fun getTheme(): String? = theme.first()

    override suspend fun getGcpProjectId(): String? = gcpProjectId.first()

    override suspend fun getGcpLocation(): String? = gcpLocation.first()

    override suspend fun getGeminiModelName(): String? = geminiModelName.first()

    override suspend fun getEnabledRoles(): Set<String> = enabledRoles.first()

    override suspend fun getGithubAccessToken(): String? = githubAccessToken.first()

    override suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = apiKey
        }
    }

    override suspend fun saveGeminiApiKey(geminiApiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = geminiApiKey
        }
    }

    override suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    override suspend fun saveGcpProjectId(gcpProjectId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GCP_PROJECT_ID] = gcpProjectId
        }
    }

    override suspend fun saveGcpLocation(gcpLocation: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GCP_LOCATION] = gcpLocation
        }
    }

    override suspend fun saveGeminiModelName(geminiModelName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_MODEL_NAME] = geminiModelName
        }
    }

    override suspend fun saveEnabledRoles(enabledRoles: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLED_ROLES] = enabledRoles
        }
    }

    override suspend fun saveGithubAccessToken(githubAccessToken: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GITHUB_ACCESS_TOKEN] = githubAccessToken
        }
    }
}