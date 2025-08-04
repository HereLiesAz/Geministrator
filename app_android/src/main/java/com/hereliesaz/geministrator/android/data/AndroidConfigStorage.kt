package com.hereliesaz.geministrator.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hereliesaz.geministrator.core.config.ConfigStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "geministrator_settings")

class AndroidConfigStorage(private val context: Context) : ConfigStorage {

    companion object {
        val API_KEY = stringPreferencesKey("GEMINI_API_KEY")
        val PRE_COMMIT_REVIEW = booleanPreferencesKey("PRE_COMMIT_REVIEW")
        val CONCURRENCY_LIMIT = intPreferencesKey("CONCURRENCY_LIMIT")
        val TOKEN_LIMIT = intPreferencesKey("TOKEN_LIMIT")
        val THEME_PREFERENCE = stringPreferencesKey("THEME_PREFERENCE")
        // Other keys can be added here
    }

    override suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { it[API_KEY] = apiKey }
    }

    override suspend fun loadApiKey(): String? {
        return context.dataStore.data.map { it[API_KEY] }.first()
    }

    override suspend fun savePreCommitReview(enabled: Boolean) {
        context.dataStore.edit { it[PRE_COMMIT_REVIEW] = enabled }
    }

    override suspend fun loadPreCommitReview(): Boolean {
        return context.dataStore.data.map { it[PRE_COMMIT_REVIEW] ?: true }.first()
    }

    // ... other required methods from ConfigStorage interface
    override fun saveModelName(type: String, name: String) = Unit // Not implemented for Android yet
    override fun loadModelName(type: String, default: String): String = default // Not implemented
    override fun saveConcurrencyLimit(limit: Int) = Unit // Not implemented
    override fun loadConcurrencyLimit(): Int = 2 // Not implemented
    override fun saveTokenLimit(limit: Int) = Unit // Not implemented
    override fun loadTokenLimit(): Int = 500000 // Not implemented
    override fun saveSearchApiKey(apiKey: String) = Unit
    override fun loadSearchApiKey(): String? = null
    override fun saveSearchEngineId(id: String) = Unit
    override fun loadSearchEngineId(): String? = null
    override fun saveAuthMethod(method: String) = Unit
    override fun loadAuthMethod(): String = "apikey"
    override fun saveFreeTierOnly(enabled: Boolean) = Unit
    override fun loadFreeTierOnly(): Boolean = false

    suspend fun saveThemePreference(theme: String) {
        context.dataStore.edit { it[THEME_PREFERENCE] = theme }
    }

    suspend fun loadThemePreference(): String? {
        return context.dataStore.data.map { it[THEME_PREFERENCE] }.first()
    }
}