package com.hereliesaz.geministrator.core.config

interface ConfigStorage {
    fun saveApiKey(apiKey: String)
    fun loadApiKey(): String?
    fun savePreCommitReview(enabled: Boolean)
    fun loadPreCommitReview(): Boolean
    fun saveModelName(type: String, name: String)
    fun loadModelName(type: String, default: String): String
    fun saveConcurrencyLimit(limit: Int)
    fun loadConcurrencyLimit(): Int
    fun saveTokenLimit(limit: Int)
    fun loadTokenLimit(): Int

    // New methods for search configuration
    fun saveSearchApiKey(apiKey: String)
    fun loadSearchApiKey(): String?
    fun saveSearchEngineId(id: String)
    fun loadSearchEngineId(): String?
}