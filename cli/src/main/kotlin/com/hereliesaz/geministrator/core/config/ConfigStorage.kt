package com.hereliesaz.geministrator.core.config

interface ConfigStorage {
    suspend fun saveApiKey(apiKey: String)
    suspend fun loadApiKey(): String?
    suspend fun savePreCommitReview(enabled: Boolean)
    suspend fun loadPreCommitReview(): Boolean
    suspend fun saveModelName(type: String, name: String)
    suspend fun loadModelName(type: String, default: String): String
    suspend fun saveConcurrencyLimit(limit: Int)
    suspend fun loadConcurrencyLimit(): Int
    suspend fun saveTokenLimit(limit: Int)
    suspend fun loadTokenLimit(): Int

    // New methods for search configuration
    suspend fun saveSearchApiKey(apiKey: String)
    suspend fun loadSearchApiKey(): String?
    suspend fun saveSearchEngineId(id: String)
    suspend fun loadSearchEngineId(): String?

    // New methods for auth
    suspend fun saveAuthMethod(method: String)
    suspend fun loadAuthMethod(): String
    suspend fun saveFreeTierOnly(enabled: Boolean)
    suspend fun loadFreeTierOnly(): Boolean

    // New method for GitHub
    suspend fun saveGitHubToken(token: String)
    suspend fun loadGitHubToken(): String?
    suspend fun saveDefaultRepo(repoName: String)
    suspend fun loadDefaultRepo(): String?
}