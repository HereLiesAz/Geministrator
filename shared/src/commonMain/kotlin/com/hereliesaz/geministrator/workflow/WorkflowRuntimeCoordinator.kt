package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.persistence.WorkflowPersistence
import com.hereliesaz.geministrator.providers.ProviderArtifact

data class WorkflowRuntimeState(
    val run: WorkflowRun,
    val handles: Map<TaskDefinitionId, ManagedSessionHandle> = emptyMap(),
)

class WorkflowRuntimeCoordinator(
    private val persistence: WorkflowPersistence,
    private val engine: WorkflowEngine,
    private val sessionGateway: ManagedSessionGateway,
) {
    suspend fun persist(
        project: Project,
        definition: WorkflowDefinition,
        state: WorkflowRuntimeState,
    ) {
        persistence.projects.put(project)
        persistence.definitions.put(definition)
        persistence.runs.put(state.run)
        state.run.taskRuns.values
            .flatMap(TaskRun::artifacts)
            .forEach { persistence.artifacts.put(it) }
    }

    suspend fun resume(workflowRunId: WorkflowRunId): WorkflowRuntimeState {
        val run = requireNotNull(persistence.runs.get(workflowRunId)) {
            "Workflow run ${workflowRunId.value} was not found"
        }
        val handles = buildMap {
            run.taskRuns.forEach { (taskDefinitionId, taskRun) ->
                val providerId = taskRun.assignedProviderId ?: return@forEach
                val providerRunId = taskRun.providerRunId ?: return@forEach
                if (!taskRun.status.canReconnect()) return@forEach

                val handle = ManagedSessionHandle(
                    taskRunId = taskRun.id,
                    providerId = providerId,
                    providerRunId = providerRunId,
                )
                sessionGateway.reconnect(handle, taskRun.status.toManagedStatus())
                put(taskDefinitionId, handle)
            }
        }
        return WorkflowRuntimeState(run = run, handles = handles)
    }

    suspend fun cycle(
        project: Project,
        definition: WorkflowDefinition,
        state: WorkflowRuntimeState,
        nowEpochMillis: Long,
        artifactIdFactory: (TaskRun, ProviderArtifact, Int) -> ArtifactId,
    ): WorkflowRuntimeState {
        var nextRun = engine.reconcile(
            definition = definition,
            run = state.run,
            handles = state.handles,
            nowEpochMillis = nowEpochMillis,
            artifactIdFactory = artifactIdFactory,
        )
        var nextHandles = state.handles.filterKeys { taskId ->
            val status = nextRun.taskRuns[taskId]?.status
            status != TaskRunStatus.Completed && status != TaskRunStatus.Failed && status != TaskRunStatus.Cancelled
        }

        var nextState = WorkflowRuntimeState(nextRun, nextHandles)
        persist(project, definition, nextState)

        if (nextRun.status.isTerminal()) return nextState

        val dispatched = engine.dispatchReadyTasks(
            project = project,
            definition = definition,
            run = nextRun,
            existingHandles = nextHandles,
            nowEpochMillis = nowEpochMillis,
        )
        nextRun = dispatched.run
        nextHandles = dispatched.handles
        nextState = WorkflowRuntimeState(nextRun, nextHandles)
        persist(project, definition, nextState)
        return nextState
    }

    private fun TaskRunStatus.canReconnect(): Boolean = when (this) {
        TaskRunStatus.Planning,
        TaskRunStatus.AwaitingApproval,
        TaskRunStatus.Running,
        TaskRunStatus.Verifying,
        -> true
        else -> false
    }

    private fun TaskRunStatus.toManagedStatus(): ManagedSessionStatus = when (this) {
        TaskRunStatus.Planning -> ManagedSessionStatus.Planning
        TaskRunStatus.AwaitingApproval -> ManagedSessionStatus.AwaitingApproval
        TaskRunStatus.Running,
        TaskRunStatus.Verifying,
        -> ManagedSessionStatus.Running
        TaskRunStatus.Completed -> ManagedSessionStatus.Completed
        TaskRunStatus.Failed -> ManagedSessionStatus.Failed
        else -> ManagedSessionStatus.Unknown
    }

    private fun WorkflowRunStatus.isTerminal(): Boolean = when (this) {
        WorkflowRunStatus.Completed,
        WorkflowRunStatus.Failed,
        WorkflowRunStatus.Cancelled,
        -> true
        else -> false
    }
}
