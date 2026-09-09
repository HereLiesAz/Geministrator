package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.domain.AgentCapability
import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ProviderRunId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.persistence.InMemoryWorkflowPersistence
import com.hereliesaz.geministrator.providers.AgentCapabilities
import com.hereliesaz.geministrator.providers.AgentEvent
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.AgentRunHandle
import com.hereliesaz.geministrator.providers.AgentTaskRequest
import com.hereliesaz.geministrator.providers.ProviderActionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StarterWorkflowEndToEndTest {
    @Test
    fun starterWorkflowRunsImplementationTestsVerificationReviewAndReleaseGate() = runBlocking {
        val persistence = InMemoryWorkflowPersistence()
        val provider = CompletingGovernedProvider()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        try {
            val runtime = ApplicationRuntime.create(
                providers = listOf(provider),
                scope = scope,
                persistence = persistence,
            )

            runtime.launchStarterWorkflow(
                projectName = "Alpha",
                objective = "Ship the requested change",
            )

            withTimeout(12_000L) {
                while (true) {
                    val live = runtime.state.value as? ApplicationRuntimeState.Live
                    val release = live?.presentation?.run?.taskRuns?.get(TaskDefinitionId("release-approval"))
                    if (live?.presentation?.run?.status == WorkflowRunStatus.AwaitingHuman &&
                        release?.status == TaskRunStatus.AwaitingApproval
                    ) break
                    delay(50L)
                }
            }

            val awaiting = assertIs<ApplicationRuntimeState.Live>(runtime.state.value)
            val taskRuns = awaiting.presentation.run.taskRuns
            assertEquals(TaskRunStatus.Completed, taskRuns.getValue(TaskDefinitionId("implementation--pre-code-tests")).status)
            assertEquals(TaskRunStatus.Completed, taskRuns.getValue(TaskDefinitionId("implementation")).status)
            assertEquals(TaskRunStatus.Completed, taskRuns.getValue(TaskDefinitionId("implementation--post-code-tests")).status)
            assertEquals(TaskRunStatus.Completed, taskRuns.getValue(TaskDefinitionId("verification")).status)
            assertEquals(TaskRunStatus.Completed, taskRuns.getValue(TaskDefinitionId("review")).status)
            assertEquals(TaskRunStatus.AwaitingApproval, taskRuns.getValue(TaskDefinitionId("release-approval")).status)
            assertTrue(provider.startedTaskIds.containsAll(
                setOf(
                    "implementation--pre-code-tests",
                    "implementation",
                    "implementation--post-code-tests",
                    "verification",
                    "review",
                ),
            ))

            runtime.approveTask(TaskDefinitionId("release-approval"))

            val completed = assertIs<ApplicationRuntimeState.Live>(runtime.state.value)
            assertEquals(WorkflowRunStatus.Completed, completed.presentation.run.status)
            assertEquals(TaskRunStatus.Completed, completed.presentation.run.taskRuns.getValue(TaskDefinitionId("release-approval")).status)
            assertEquals(WorkflowRunStatus.Completed, persistence.runs.get(completed.presentation.run.id)?.status)
        } finally {
            scope.cancel()
        }
    }
}

private class CompletingGovernedProvider : AgentProvider {
    override val id = AgentProviderId("governed-provider")
    val startedTaskIds = mutableSetOf<String>()
    private var runCount = 0

    override suspend fun capabilities() = AgentCapabilities(
        supported = setOf(
            AgentCapability.RepositoryRead,
            AgentCapability.RepositoryWrite,
            AgentCapability.TestAuthoring,
            AgentCapability.Testing,
        ),
    )

    override suspend fun start(request: AgentTaskRequest): AgentRunHandle {
        startedTaskIds += request.taskRunId.value.substringAfterLast(":", request.taskRunId.value)
        runCount += 1
        return AgentRunHandle(ProviderRunId("governed-run-$runCount"))
    }

    override fun observe(runId: ProviderRunId): Flow<AgentEvent> = flowOf(AgentEvent.Completed(runId))

    override suspend fun sendMessage(runId: ProviderRunId, message: String) = ProviderActionResult.Accepted

    override suspend fun approvePlan(runId: ProviderRunId) = ProviderActionResult.Accepted

    override suspend fun cancel(runId: ProviderRunId) = ProviderActionResult.Accepted
}
