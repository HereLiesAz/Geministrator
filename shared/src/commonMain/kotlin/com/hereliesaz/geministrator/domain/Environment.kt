package com.hereliesaz.geministrator.domain

enum class EnvironmentPlanningPolicy {
    NotRequired,
    WhenProviderRequires,
    Always,
}

enum class NetworkAccessPolicy {
    None,
    Restricted,
    Internet,
}

enum class IsolationLevel {
    Shared,
    Isolated,
    Ephemeral,
}

data class EnvironmentSpecification(
    val runtime: String? = null,
    val runtimeVersion: String? = null,
    val tools: Set<String> = emptySet(),
    val services: Set<String> = emptySet(),
    val requiredEnvironmentVariables: Set<String> = emptySet(),
    val requiredSecretNames: Set<String> = emptySet(),
    val networkAccess: NetworkAccessPolicy = NetworkAccessPolicy.Restricted,
    val isolation: IsolationLevel = IsolationLevel.Ephemeral,
    val cpuHint: String? = null,
    val memoryHint: String? = null,
    val notes: List<String> = emptyList(),
)
