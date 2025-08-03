package com.hereliesaz.geministrator.adapter

import com.hereliesaz.geministrator.core.config.ConfigStorage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class CliConfigStorage : ConfigStorage {
    private val configFile = File(System.getProperty("user.home"), ".gemini-orchestrator/config.properties")
    private val properties = Properties()
    private val KEY_API = "GEMINI_API_KEY"
    private val KEY_REVIEW = "PRE_COMMIT_REVIEW"
    private val KEY_CONCURRENCY = "CONCURRENCY_LIMIT"
    private val KEY_TOKEN_LIMIT = "TOKEN_LIMIT"
    private val KEY_MODEL_STRATEGIC = "MODEL_STRATEGIC"
    private val KEY_MODEL_FLASH = "MODEL_FLASH"
    private val KEY_SEARCH_API = "SEARCH_API_KEY"
    private val KEY_SEARCH_ENGINE_ID = "SEARCH_ENGINE_ID"

    init {
        configFile.parentFile.mkdirs()
        if (configFile.exists()) { FileInputStream(configFile).use { properties.load(it) } }
    }

    private fun saveProperties() { FileOutputStream(configFile).use { properties.store(it, "Gemini Orchestrator Configuration") } }
    override fun saveApiKey(apiKey: String) { properties.setProperty(KEY_API, apiKey); saveProperties() }
    override fun loadApiKey(): String? = properties.getProperty(KEY_API)
    override fun savePreCommitReview(enabled: Boolean) { properties.setProperty(KEY_REVIEW, enabled.toString()); saveProperties() }
    override fun loadPreCommitReview(): Boolean = properties.getProperty(KEY_REVIEW, "true").toBoolean()
    override fun saveConcurrencyLimit(limit: Int) { properties.setProperty(KEY_CONCURRENCY, limit.toString()); saveProperties() }
    override fun loadConcurrencyLimit(): Int = properties.getProperty(KEY_CONCURRENCY, "2").toIntOrNull() ?: 2
    override fun saveTokenLimit(limit: Int) { properties.setProperty(KEY_TOKEN_LIMIT, limit.toString()); saveProperties() }
    override fun loadTokenLimit(): Int = properties.getProperty(KEY_TOKEN_LIMIT, "500000").toIntOrNull() ?: 500000
    override fun saveModelName(type: String, name: String) { properties.setProperty(if (type == "strategic") KEY_MODEL_STRATEGIC else KEY_MODEL_FLASH, name); saveProperties() }
    override fun loadModelName(type: String, default: String): String = properties.getProperty(if (type == "strategic") KEY_MODEL_STRATEGIC else KEY_MODEL_FLASH, default)

    // Implementation for new search config methods
    override fun saveSearchApiKey(apiKey: String) {
        properties.setProperty(KEY_SEARCH_API, apiKey); saveProperties()
    }

    override fun loadSearchApiKey(): String? = properties.getProperty(KEY_SEARCH_API)
    override fun saveSearchEngineId(id: String) {
        properties.setProperty(KEY_SEARCH_ENGINE_ID, id); saveProperties()
    }

    override fun loadSearchEngineId(): String? = properties.getProperty(KEY_SEARCH_ENGINE_ID)
}