package com.hereliesaz.Geministrator.plugins

import com.hereliesaz.Geministrator.core.config.ConfigStorage
import com.intellij.ide.util.PropertiesComponent

class PluginConfigStorage : ConfigStorage {
    private val props = PropertiesComponent.getInstance()
    private val ID_API_KEY = "com.hereliesaz.GeminiOrchestrator.apiKey"
    private val ID_REVIEW_ENABLED = "com.hereliesaz.GeminiOrchestrator.reviewEnabled"
    private val ID_CONCURRENCY_LIMIT = "com.hereliesaz.GeminiOrchestrator.concurrencyLimit"
    private val ID_TOKEN_LIMIT = "com.hereliesaz.GeminiOrchestrator.tokenLimit"
    private val ID_MODEL_STRATEGIC = "com.hereliesaz.GeminiOrchestrator.modelStrategic"
    private val ID_MODEL_FLASH = "com.hereliesaz.GeminiOrchestrator.modelFlash"

    override fun saveApiKey(apiKey: String) = props.setValue(ID_API_KEY, apiKey)
    override fun loadApiKey(): String? = props.getValue(ID_API_KEY)
    override fun savePreCommitReview(enabled: Boolean) = props.setValue(ID_REVIEW_ENABLED, enabled, true)
    override fun loadPreCommitReview(): Boolean = props.getBoolean(ID_REVIEW_ENABLED, true)
    override fun saveConcurrencyLimit(limit: Int) = props.setValue(ID_CONCURRENCY_LIMIT, limit, 2)
    override fun loadConcurrencyLimit(): Int = props.getInt(ID_CONCURRENCY_LIMIT, 2)
    override fun saveTokenLimit(limit: Int) = props.setValue(ID_TOKEN_LIMIT, limit, 500000)
    override fun loadTokenLimit(): Int = props.getInt(ID_TOKEN_LIMIT, 500000)
    override fun saveModelName(type: String, name: String) = props.setValue(if (type == "strategic") ID_MODEL_STRATEGIC else ID_MODEL_FLASH, name)
    override fun loadModelName(type: String, default: String): String = props.getValue(if (type == "strategic") ID_MODEL_STRATEGIC else ID_MODEL_FLASH, default)
}