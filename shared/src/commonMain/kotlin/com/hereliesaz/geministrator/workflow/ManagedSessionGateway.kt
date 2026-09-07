package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.providers.AgentTaskRequest
import com.hereliesaz.geministrator.providers.ProviderActionResult

data class ManagedSessionHandle(
    val taskRunId: TaskRunId,
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
    suspend fun createSession(request: AgentTaskRequest): ManagedSessionHandle

    suspend fun status(handle: ManagedSessionHandle): ManagedSessionStatus

    suspend fun message(
        handle: ManagedSessionHandle,
        message: String,
    ): ProviderActionResult

    suspend fun approvePlan(handle: ManagedSessionHandle): ProviderActionResult

    suspend fun artifacts(handle: ManagedSessionHandle): List<ArtifactRef>
}
