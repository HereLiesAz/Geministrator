package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentCapability
import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.providers.AgentCapabilities
import com.hereliesaz.geministrator.providers.AgentEvent
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.AgentRunHandle
import com.hereliesaz.geministrator.providers.AgentTaskRequest
import com.hereliesaz.geministrator.providers.ProviderActionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProviderBackedManagedSessionGatewayTest {
    @Test
    fun providerSelectionPreservesCancellation() {
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val gateway = ProviderBackedManagedSessionGateway(
                    AgentProviderRegistry(listOf(CancellingCapabilitiesProvider())),
                    scope,
                )

                assertFailsWith<CancellationException> {
                    gateway.resolveProvider(ProviderSelectionRequest())
                }
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun observerTransportFailurePreservesRemoteRunAndRetriesObservation() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val provider = RetryingObserverProvider()
        val handle = ManagedSessionHandle(
            taskRunId = TaskRunId("task-run"),
            providerId = provider.id,
            providerRunId = ProviderRunId("provider-run"),
        )
        try {
            val gateway = ProviderBackedManagedSessionGateway(
                AgentProviderRegistry(listOf(provider)),
                scope,
            )

            gateway.reconnect(handle, ManagedSessionStatus.Running)
            withTimeout(2_000L) { provider.firstObservationFailed.await() }

            assertEquals(ManagedSessionStatus.Running, gateway.status(handle))

            withTimeout(4_000L) {
                while (gateway.status(handle) != ManagedSessionStatus.Completed) {
                    delay(25L)
                }
            }
            assertTrue(provider.observeCount >= 2)
            assertEquals(ManagedSessionStatus.Completed, gateway.status(handle))
        } finally {
            scope.cancel()
        }
    }
}

private class CancellingCapabilitiesProvider : AgentProvider {
    override val id = AgentProviderId("cancel-provider")

    override suspend fun capabilities(): AgentCapabilities = throw CancellationException("cancel selection")

    override suspend fun start(request: AgentTaskRequest): AgentRunHandle = error("not called")

    override fun observe(runId: ProviderRunId): Flow<AgentEvent> = flow { error("not called") }

    override suspend fun sendMessage(runId: ProviderRunId, message: String) = ProviderActionResult.Accepted

    override suspend fun approvePlan(runId: ProviderRunId) = ProviderActionResult.Accepted

    override suspend fun cancel(runId: ProviderRunId) = ProviderActionResult.Accepted
}

private class RetryingObserverProvider : AgentProvider {
    override val id = AgentProviderId("retry-observer")
    var observeCount = 0
    val firstObservationFailed = CompletableDeferred<Unit>()

    override suspend fun capabilities() = AgentCapabilities(
        supported = setOf(AgentCapability.RepositoryRead),
    )

    override suspend fun start(request: AgentTaskRequest) = AgentRunHandle(ProviderRunId("provider-run"))

    override fun observe(runId: ProviderRunId): Flow<AgentEvent> = flow {
        observeCount += 1
        if (observeCount == 1) {
            firstObservationFailed.complete(Unit)
            throw IllegalStateException("temporary transport failure")
        }
        emit(AgentEvent.Completed(runId))
    }

    override suspend fun sendMessage(runId: ProviderRunId, message: String) = ProviderActionResult.Accepted

    override suspend fun approvePlan(runId: ProviderRunId) = ProviderActionResult.Accepted

    override suspend fun cancel(runId: ProviderRunId) = ProviderActionResult.Accepted
}
