package com.hereliesaz.geministrator_plugin.adapter

import com.hereliesaz.geministrator.core.config.ConfigStorage
import com.intellij.ide.util.PropertiesComponent

class PluginConfigStorage : ConfigStorage {
    private val props = PropertiesComponent.getInstance()
    private val ID_API_KEY = "com.hereliesaz.Geministrator.apiKey"
    private val ID_REVIEW_ENABLED = "com.hereliesaz.Geministrator.reviewEnabled"
    private val ID_CONCURRENCY_LIMIT = "com.hereliesaz.Geministrator.concurrencyLimit"
    private val ID_TOKEN_LIMIT = "com.hereliesaz.Geministrator.tokenLimit"
    private val ID_MODEL_STRATEGIC = "com.hereliesaz.Geministrator.modelStrategic"
    private val ID_MODEL_FLASH = "com.hereliesaz.Geministrator.modelFlash"
    private val ID_SEARCH_API_KEY = "com.hereliesaz.Geministrator.searchApiKey"
    private val ID_SEARCH_ENGINE_ID = "com.hereliesaz.Geministrator.searchEngineId"
    private val ID_AUTH_METHOD = "com.hereliesaz.Geministrator.authMethod"
    private val ID_FREE_TIER_ONLY = "com.hereliesaz.Geministrator.freeTierOnly"

    override suspend fun saveApiKey(apiKey: String) = props.setValue(ID_API_KEY, apiKey)
    override suspend fun loadApiKey(): String? = props.getValue(ID_API_KEY)

    override suspend fun savePreCommitReview(enabled: Boolean) =
        props.setValue(ID_REVIEW_ENABLED, enabled, true)

    override suspend fun loadPreCommitReview(): Boolean = props.getBoolean(ID_REVIEW_ENABLED, true)

    override suspend fun saveConcurrencyLimit(limit: Int) =
        props.setValue(ID_CONCURRENCY_LIMIT, limit, 2)

    override suspend fun loadConcurrencyLimit(): Int = props.getInt(ID_CONCURRENCY_LIMIT, 2)

    override suspend fun saveTokenLimit(limit: Int) = props.setValue(ID_TOKEN_LIMIT, limit, 500000)
    override suspend fun loadTokenLimit(): Int = props.getInt(ID_TOKEN_LIMIT, 500000)

    override suspend fun saveModelName(type: String, name: String) =
        props.setValue(if (type == "strategic") ID_MODEL_STRATEGIC else ID_MODEL_FLASH, name)

    override suspend fun loadModelName(type: String, default: String): String =
        props.getValue(if (type == "strategic") ID_MODEL_STRATEGIC else ID_MODEL_FLASH, default)

    override suspend fun saveSearchApiKey(apiKey: String) =
        props.setValue(ID_SEARCH_API_KEY, apiKey)

    override suspend fun loadSearchApiKey(): String? = props.getValue(ID_SEARCH_API_KEY)

    override suspend fun saveSearchEngineId(id: String) = props.setValue(ID_SEARCH_ENGINE_ID, id)
    override suspend fun loadSearchEngineId(): String? = props.getValue(ID_SEARCH_ENGINE_ID)

    override suspend fun saveAuthMethod(method: String) =
        props.setValue(ID_AUTH_METHOD, method, "apikey")

    override suspend fun loadAuthMethod(): String = props.getValue(ID_AUTH_METHOD, "apikey")

    override suspend fun saveFreeTierOnly(enabled: Boolean) =
        props.setValue(ID_FREE_TIER_ONLY, enabled, false)

    override suspend fun loadFreeTierOnly(): Boolean = props.getBoolean(ID_FREE_TIER_ONLY, false)

    override suspend fun loadDefaultRepo(): String? = null
    override suspend fun saveGitHubToken(token: String) {}
    override suspend fun loadGitHubToken(): String? = null
    override suspend fun saveDefaultRepo(repoName: String) {}
}