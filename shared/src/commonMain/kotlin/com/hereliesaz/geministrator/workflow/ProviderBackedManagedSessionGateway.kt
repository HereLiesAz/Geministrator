package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.providers.AgentEvent
import com.hereliesaz.geministrator.providers.ProviderActionResult
import com.hereliesaz.geministrator.providers.ProviderArtifact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProviderBackedManagedSessionGateway(
    private val providerRegistry: AgentProviderRegistry,
    private val scope: CoroutineScope,
) : ManagedSessionGateway {

    private data class SessionSnapshot(
        val status: ManagedSessionStatus,
        val artifacts: List<ProviderArtifact> = emptyList(),
    )

    private val mutex = Mutex()
    private val snapshots = mutableMapOf<ManagedSessionHandle, SessionSnapshot>()

    override suspend fun createSession(request: ManagedSessionRequest): ManagedSessionHandle {
        val provider = providerRegistry.select(request.providerSelection)
        val run = provider.start(request.taskRequest)
        val handle = ManagedSessionHandle(
            taskRunId = request.taskRequest.taskRunId,
            providerId = provider.id,
            providerRunId = run.providerRunId,
        )

        mutex.withLock {
            snapshots[handle] = SessionSnapshot(
                status = if (request.taskRequest.requirePlanApproval) {
                    ManagedSessionStatus.Planning
                } else {
                    ManagedSessionStatus.Running
                },
            )
        }

        scope.launch {
            provider.observe(run.providerRunId).collect { event ->
                applyEvent(handle, event)
            }
        }

        return handle
    }

    override suspend fun status(handle: ManagedSessionHandle): ManagedSessionStatus =
        mutex.withLock { snapshots[handle]?.status ?: ManagedSessionStatus.Unknown }

    override suspend fun message(
        handle: ManagedSessionHandle,
        message: String,
    ): ProviderActionResult = providerFor(handle).sendMessage(handle.providerRunId, message)

    override suspend fun approvePlan(handle: ManagedSessionHandle): ProviderActionResult {
        val result = providerFor(handle).approvePlan(handle.providerRunId)
        if (result is ProviderActionResult.Accepted) {
            mutex.withLock {
                val current = snapshots[handle] ?: return@withLock
                snapshots[handle] = current.copy(status = ManagedSessionStatus.Running)
            }
        }
        return result
    }

    override suspend fun artifacts(handle: ManagedSessionHandle): List<ProviderArtifact> =
        mutex.withLock { snapshots[handle]?.artifacts.orEmpty() }

    private fun providerFor(handle: ManagedSessionHandle) =
        requireNotNull(providerRegistry.provider(handle.providerId)) {
            "Provider ${handle.providerId.value} is no longer registered"
        }

    private suspend fun applyEvent(
        handle: ManagedSessionHandle,
        event: AgentEvent,
    ) {
        mutex.withLock {
            val current = snapshots[handle] ?: SessionSnapshot(ManagedSessionStatus.Unknown)
            val next = when (event) {
                is AgentEvent.PlanGenerated -> current.copy(status = ManagedSessionStatus.AwaitingApproval)
                is AgentEvent.PlanApproved -> current.copy(status = ManagedSessionStatus.Running)
                is AgentEvent.Progress -> current.copy(status = ManagedSessionStatus.Running)
                is AgentEvent.Message -> current
                is AgentEvent.ArtifactProduced -> current.copy(
                    artifacts = current.artifacts + event.artifact,
                )
                is AgentEvent.Completed -> current.copy(status = ManagedSessionStatus.Completed)
                is AgentEvent.Failed -> current.copy(status = ManagedSessionStatus.Failed)
            }
            snapshots[handle] = next
        }
    }
}
