package com.hereliesaz.geministrator.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val apiKey: Flow<String?>
    val geminiApiKey: Flow<String?>
    val theme: Flow<String?>
    val gcpProjectId: Flow<String?>
    val gcpLocation: Flow<String?>
    val geminiModelName: Flow<String?>
    val enabledRoles: Flow<Set<String>>

    suspend fun getApiKey(): String?
    suspend fun getGeminiApiKey(): String?
    suspend fun getTheme(): String?
    suspend fun getGcpProjectId(): String?
    suspend fun getGcpLocation(): String?
    suspend fun getGeminiModelName(): String?
    suspend fun getEnabledRoles(): Set<String>

    suspend fun saveApiKey(apiKey: String)
    suspend fun saveGeminiApiKey(geminiApiKey: String)
    suspend fun saveTheme(theme: String)
    suspend fun saveGcpProjectId(gcpProjectId: String)
    suspend fun saveGcpLocation(gcpLocation: String)
    suspend fun saveGeminiModelName(geminiModelName: String)
    suspend fun saveEnabledRoles(enabledRoles: Set<String>)
}
