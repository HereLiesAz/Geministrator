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

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private object PreferenceKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val THEME = stringPreferencesKey("theme")
        val GCP_PROJECT_ID = stringPreferencesKey("gcp_project_id")
        val GCP_LOCATION = stringPreferencesKey("gcp_location")
        val GEMINI_MODEL_NAME = stringPreferencesKey("gemini_model_name")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val PROFILE_PICTURE_URL = stringPreferencesKey("profile_picture_url")
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

    val userId: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.USER_ID]
        }

    val username: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.USERNAME]
        }

    val profilePictureUrl: Flow<String?>
        get() = context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.PROFILE_PICTURE_URL]
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

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.USER_ID] = userId
        }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.USERNAME] = username
        }
    }

    suspend fun saveProfilePictureUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.PROFILE_PICTURE_URL] = url
        }
    }

    suspend fun saveEnabledRoles(enabledRoles: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.ENABLED_ROLES] = enabledRoles
        }
    }
}
