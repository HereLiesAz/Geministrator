package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.providers.AgentEvent
import com.hereliesaz.geministrator.providers.AgentProvider
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
        val progress: ManagedSessionProgress? = null,
    )

    private val mutex = Mutex()
    private val snapshots = mutableMapOf<ManagedSessionHandle, SessionSnapshot>()

    override suspend fun resolveProvider(selection: ProviderSelectionRequest): AgentProviderId =
        providerCall("No registered provider can satisfy this task") {
            providerRegistry.select(selection).id
        }

    override suspend fun createSession(request: ManagedSessionRequest): ManagedSessionHandle =
        providerCall("Unable to start provider session") {
            val provider = providerRegistry.select(request.providerSelection)
            val run = provider.start(request.taskRequest)
            val handle = ManagedSessionHandle(
                taskRunId = request.taskRequest.taskRunId,
                providerId = provider.id,
                providerRunId = run.providerRunId,
            )

            registerAndObserve(
                handle = handle,
                initialStatus = if (request.taskRequest.requirePlanApproval) {
                    ManagedSessionStatus.Planning
                } else {
                    ManagedSessionStatus.Running
                },
            )

            handle
        }

    override suspend fun reconnect(
        handle: ManagedSessionHandle,
        initialStatus: ManagedSessionStatus,
    ) {
        providerCall("Unable to reconnect provider session ${handle.providerRunId.value}") {
            registerAndObserve(handle, initialStatus)
        }
    }

    override suspend fun status(handle: ManagedSessionHandle): ManagedSessionStatus =
        mutex.withLock { snapshots[handle]?.status ?: ManagedSessionStatus.Unknown }

    override suspend fun progress(handle: ManagedSessionHandle): ManagedSessionProgress? =
        mutex.withLock { snapshots[handle]?.progress }

    override suspend fun message(
        handle: ManagedSessionHandle,
        message: String,
    ): ProviderActionResult = providerCall("Unable to message provider session ${handle.providerRunId.value}") {
        providerFor(handle).sendMessage(handle.providerRunId, message)
    }

    override suspend fun approvePlan(handle: ManagedSessionHandle): ProviderActionResult =
        providerCall("Unable to approve provider session ${handle.providerRunId.value}") {
            val result = providerFor(handle).approvePlan(handle.providerRunId)
            if (result is ProviderActionResult.Accepted) {
                mutex.withLock {
                    val current = snapshots[handle] ?: return@withLock
                    snapshots[handle] = current.copy(status = ManagedSessionStatus.Running)
                }
            }
            result
        }

    override suspend fun artifacts(handle: ManagedSessionHandle): List<ProviderArtifact> =
        mutex.withLock { snapshots[handle]?.artifacts.orEmpty() }

    private suspend fun registerAndObserve(
        handle: ManagedSessionHandle,
        initialStatus: ManagedSessionStatus,
    ) {
        val shouldObserve = mutex.withLock {
            if (snapshots.containsKey(handle)) {
                false
            } else {
                snapshots[handle] = SessionSnapshot(initialStatus)
                true
            }
        }
        if (!shouldObserve) return

        val provider = providerFor(handle)
        scope.launch {
            provider.observe(handle.providerRunId).collect { event ->
                applyEvent(handle, event)
            }
        }
    }

    private fun providerFor(handle: ManagedSessionHandle): AgentProvider =
        providerRegistry.provider(handle.providerId)
            ?: throw ManagedSessionFailure.ProviderUnavailable(
                "Provider ${handle.providerId.value} is no longer registered",
            )

    private suspend fun <T> providerCall(
        fallbackMessage: String,
        block: suspend () -> T,
    ): T = try {
        block()
    } catch (failure: ManagedSessionFailure.ProviderUnavailable) {
        throw failure
    } catch (failure: Throwable) {
        throw ManagedSessionFailure.ProviderUnavailable(
            failure.message?.takeIf(String::isNotBlank) ?: fallbackMessage,
            failure,
        )
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
                is AgentEvent.Progress -> current.copy(
                    status = ManagedSessionStatus.Running,
                    progress = ManagedSessionProgress(
                        fraction = event.fraction,
                        message = event.message.takeIf { it.isNotBlank() },
                    ),
                )
                is AgentEvent.Message -> current
                is AgentEvent.ArtifactProduced -> current.copy(
                    artifacts = current.artifacts + event.artifact,
                )
                is AgentEvent.Completed -> current.copy(
                    status = ManagedSessionStatus.Completed,
                    progress = ManagedSessionProgress(
                        fraction = 1f,
                        message = current.progress?.message,
                    ),
                )
                is AgentEvent.Failed -> current.copy(status = ManagedSessionStatus.Failed)
            }
            snapshots[handle] = next
        }
    }
}
