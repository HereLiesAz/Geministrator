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
        val SEARCH_API_KEY = stringPreferencesKey("SEARCH_API_KEY")
        val SEARCH_ENGINE_ID = stringPreferencesKey("SEARCH_ENGINE_ID")
        val AUTH_METHOD = stringPreferencesKey("AUTH_METHOD")
        val FREE_TIER_ONLY = booleanPreferencesKey("FREE_TIER_ONLY")
        val MODEL_STRATEGIC = stringPreferencesKey("MODEL_STRATEGIC")
        val MODEL_FLASH = stringPreferencesKey("MODEL_FLASH")
        val GITHUB_TOKEN = stringPreferencesKey("GITHUB_TOKEN")
        val DEFAULT_REPO = stringPreferencesKey("DEFAULT_GITHUB_REPO")
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

    override suspend fun saveModelName(type: String, name: String) {
        val key = if (type == "strategic") MODEL_STRATEGIC else MODEL_FLASH
        context.dataStore.edit { it[key] = name }
    }

    override suspend fun loadModelName(type: String, default: String): String {
        val key = if (type == "strategic") MODEL_STRATEGIC else MODEL_FLASH
        return context.dataStore.data.map { it[key] ?: default }.first()
    }

    override suspend fun saveConcurrencyLimit(limit: Int) {
        context.dataStore.edit { it[CONCURRENCY_LIMIT] = limit }
    }

    override suspend fun loadConcurrencyLimit(): Int {
        return context.dataStore.data.map { it[CONCURRENCY_LIMIT] ?: 2 }.first()
    }

    override suspend fun saveTokenLimit(limit: Int) {
        context.dataStore.edit { it[TOKEN_LIMIT] = limit }
    }

    override suspend fun loadTokenLimit(): Int {
        return context.dataStore.data.map { it[TOKEN_LIMIT] ?: 500000 }.first()
    }

    override suspend fun saveSearchApiKey(apiKey: String) {
        context.dataStore.edit { it[SEARCH_API_KEY] = apiKey }
    }

    override suspend fun loadSearchApiKey(): String? {
        return context.dataStore.data.map { it[SEARCH_API_KEY] }.first()
    }

    override suspend fun saveSearchEngineId(id: String) {
        context.dataStore.edit { it[SEARCH_ENGINE_ID] = id }
    }

    override suspend fun loadSearchEngineId(): String? {
        return context.dataStore.data.map { it[SEARCH_ENGINE_ID] }.first()
    }

    override suspend fun saveAuthMethod(method: String) {
        context.dataStore.edit { it[AUTH_METHOD] = method }
    }

    override suspend fun loadAuthMethod(): String {
        return context.dataStore.data.map { it[AUTH_METHOD] ?: "apikey" }.first()
    }

    override suspend fun saveFreeTierOnly(enabled: Boolean) {
        context.dataStore.edit { it[FREE_TIER_ONLY] = enabled }
    }

    override suspend fun loadFreeTierOnly(): Boolean {
        return context.dataStore.data.map { it[FREE_TIER_ONLY] ?: false }.first()
    }

    override suspend fun saveGitHubToken(token: String) {
        context.dataStore.edit { it[GITHUB_TOKEN] = token }
    }

    override suspend fun loadGitHubToken(): String? {
        return context.dataStore.data.map { it[GITHUB_TOKEN] }.first()
    }

    override suspend fun saveDefaultRepo(repoName: String) {
        context.dataStore.edit { it[DEFAULT_REPO] = repoName }
    }

    override suspend fun loadDefaultRepo(): String? {
        return context.dataStore.data.map { it[DEFAULT_REPO] }.first()
    }

    suspend fun saveThemePreference(theme: String) {
        context.dataStore.edit { it[THEME_PREFERENCE] = theme }
    }

    suspend fun loadThemePreference(): String? {
        return context.dataStore.data.map { it[THEME_PREFERENCE] }.first()
    }
}