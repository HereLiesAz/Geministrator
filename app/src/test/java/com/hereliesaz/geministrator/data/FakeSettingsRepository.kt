package com.hereliesaz.geministrator.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeSettingsRepository : SettingsRepository {
    private val apiKeyFlow = MutableStateFlow<String?>("jules_api_key")
    private val geminiApiKeyFlow = MutableStateFlow<String?>("gemini_api_key")
    private val themeFlow = MutableStateFlow<String?>("System")
    private val gcpProjectIdFlow = MutableStateFlow<String?>("gcp_project_id")
    private val gcpLocationFlow = MutableStateFlow<String?>("gcp_location")
    private val geminiModelNameFlow = MutableStateFlow<String?>("gemini_model_name")
    private val enabledRolesFlow = MutableStateFlow<Set<String>>(emptySet())

    override val apiKey: Flow<String?> = apiKeyFlow
    override val geminiApiKey: Flow<String?> = geminiApiKeyFlow
    override val theme: Flow<String?> = themeFlow
    override val gcpProjectId: Flow<String?> = gcpProjectIdFlow
    override val gcpLocation: Flow<String?> = gcpLocationFlow
    override val geminiModelName: Flow<String?> = geminiModelNameFlow
    override val enabledRoles: Flow<Set<String>> = enabledRolesFlow

    override suspend fun saveApiKey(apiKey: String) {
        apiKeyFlow.value = apiKey
    }

    override suspend fun saveGeminiApiKey(geminiApiKey: String) {
        geminiApiKeyFlow.value = geminiApiKey
    }

    override suspend fun saveTheme(theme: String) {
        themeFlow.value = theme
    }

    override suspend fun saveGcpProjectId(gcpProjectId: String) {
        gcpProjectIdFlow.value = gcpProjectId
    }

    override suspend fun saveGcpLocation(gcpLocation: String) {
        gcpLocationFlow.value = gcpLocation
    }

    override suspend fun saveGeminiModelName(geminiModelName: String) {
        geminiModelNameFlow.value = geminiModelName
    }

    override suspend fun saveEnabledRoles(enabledRoles: Set<String>) {
        enabledRolesFlow.value = enabledRoles
    }
}
