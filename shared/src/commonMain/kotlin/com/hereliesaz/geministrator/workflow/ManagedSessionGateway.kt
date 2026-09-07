package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.providers.AgentTaskRequest
import com.hereliesaz.geministrator.providers.ProviderActionResult
import com.hereliesaz.geministrator.providers.ProviderArtifact

data class ManagedSessionRequest(
    val providerSelection: ProviderSelectionRequest,
    val taskRequest: AgentTaskRequest,
)

data class ManagedSessionHandle(
    val taskRunId: TaskRunId,
    val providerId: AgentProviderId,
    val providerRunId: ProviderRunId,
)

enum class ManagedSessionStatus {
    Planning,
    AwaitingApproval,
    Running,
    Completed,
    Failed,
    Unknown,
}

interface ManagedSessionGateway {
    suspend fun resolveProvider(selection: ProviderSelectionRequest): AgentProviderId

    suspend fun createSession(request: ManagedSessionRequest): ManagedSessionHandle

    suspend fun reconnect(
        handle: ManagedSessionHandle,
        initialStatus: ManagedSessionStatus,
    )

    suspend fun status(handle: ManagedSessionHandle): ManagedSessionStatus

    suspend fun message(
        handle: ManagedSessionHandle,
        message: String,
    ): ProviderActionResult

    suspend fun approvePlan(handle: ManagedSessionHandle): ProviderActionResult

    suspend fun artifacts(handle: ManagedSessionHandle): List<ProviderArtifact>
}
