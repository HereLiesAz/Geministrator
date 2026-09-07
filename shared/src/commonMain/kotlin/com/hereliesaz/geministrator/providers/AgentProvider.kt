package com.hereliesaz.geministrator.providers

import com.hereliesaz.geministrator.domain.AcceptanceCriterion
import com.hereliesaz.geministrator.domain.AgentCapability
import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.PromptReusePolicy
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.RepositoryRef
import com.hereliesaz.geministrator.domain.TaskRunId
import kotlinx.coroutines.flow.Flow

enum class PromptCacheMode {
    Unsupported,
    ImplicitPrefix,
    ExplicitReusableContext,
    ExplicitBreakpoints,
    SessionScoped,
}

data class PromptCacheCapabilities(
    val modes: Set<PromptCacheMode> = setOf(PromptCacheMode.Unsupported),
    val reportsCacheUsage: Boolean = false,
)

data class AgentCapabilities(
    val supported: Set<AgentCapability>,
    val promptCaching: PromptCacheCapabilities = PromptCacheCapabilities(),
)

data class PromptContextBlock(
    val label: String,
    val content: String,
)

data class PromptContext(
    val stablePrefix: List<PromptContextBlock> = emptyList(),
    val dynamicContext: List<PromptContextBlock> = emptyList(),
    val reusePolicy: PromptReusePolicy = PromptReusePolicy.ProviderDefault,
    val cacheNamespace: String? = null,
)

data class AgentTaskRequest(
    val taskRunId: TaskRunId,
    val objective: String,
    val roleInstructions: String,
    val acceptanceCriteria: List<AcceptanceCriterion>,
    val contextArtifacts: List<ArtifactRef> = emptyList(),
    val repository: RepositoryRef? = null,
    val isolationHint: IsolationHint = IsolationHint.ProviderDefault,
    val requirePlanApproval: Boolean = false,
    val promptContext: PromptContext = PromptContext(),
)

enum class IsolationHint {
    ProviderDefault,
    DedicatedBranch,
    DedicatedWorkspace,
    Repoless,
}

data class AgentRunHandle(
    val providerRunId: ProviderRunId,
)

sealed interface ProviderActionResult {
    data object Accepted : ProviderActionResult
    data class Rejected(val reason: String) : ProviderActionResult
}

sealed interface AgentEvent {
    val runId: ProviderRunId

    data class PlanGenerated(
        override val runId: ProviderRunId,
        val summary: String,
    ) : AgentEvent

    data class Progress(
        override val runId: ProviderRunId,
        val message: String,
    ) : AgentEvent

    data class ArtifactProduced(
        override val runId: ProviderRunId,
        val artifact: ArtifactRef,
    ) : AgentEvent

    data class Completed(
        override val runId: ProviderRunId,
    ) : AgentEvent

    data class Failed(
        override val runId: ProviderRunId,
        val reason: String,
    ) : AgentEvent
}

interface AgentProvider {
    val id: AgentProviderId

    suspend fun capabilities(): AgentCapabilities

    suspend fun start(request: AgentTaskRequest): AgentRunHandle

    fun observe(runId: ProviderRunId): Flow<AgentEvent>

    suspend fun sendMessage(
        runId: ProviderRunId,
        message: String,
    ): ProviderActionResult

    suspend fun approvePlan(runId: ProviderRunId): ProviderActionResult

    suspend fun cancel(runId: ProviderRunId): ProviderActionResult
}
