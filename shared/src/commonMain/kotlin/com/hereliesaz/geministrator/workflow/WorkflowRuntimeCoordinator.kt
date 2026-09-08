package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.BlockingReason
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.RetryReason
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
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
        if (state.run.status == WorkflowRunStatus.AwaitingHuman && state.handles.isEmpty()) {
            return state
        }

        var nextRun = engine.reconcile(
            definition = definition,
            run = state.run,
            handles = state.handles,
            nowEpochMillis = nowEpochMillis,
            artifactIdFactory = artifactIdFactory,
        )
        nextRun = preserveArtifactTimestamps(previous = state.run, reconciled = nextRun)

        val newlyFailedTaskIds = nextRun.taskRuns
            .filter { (taskId, taskRun) ->
                taskRun.status == TaskRunStatus.Failed && state.run.taskRuns[taskId]?.status != TaskRunStatus.Failed
            }
            .keys

        for (taskId in newlyFailedTaskIds) {
            nextRun = engine.handleFailure(
                definition = definition,
                run = nextRun,
                taskDefinitionId = taskId,
                retryReason = RetryReason.ProviderFailure,
                reason = "Provider session failed",
                nowEpochMillis = nowEpochMillis,
            )
        }

        if (
            state.run.status == WorkflowRunStatus.AwaitingHuman ||
            nextRun.taskRuns.values.any { it.status == TaskRunStatus.AwaitingApproval || it.status == TaskRunStatus.Escalated }
        ) {
            nextRun = nextRun.copy(status = WorkflowRunStatus.AwaitingHuman)
        }

        var nextHandles = state.handles.filterKeys { taskId ->
            val status = nextRun.taskRuns[taskId]?.status
            status != TaskRunStatus.Completed &&
                status != TaskRunStatus.Failed &&
                status != TaskRunStatus.Cancelled &&
                status != TaskRunStatus.Retrying &&
                status != TaskRunStatus.Escalated
        }

        nextRun = blockUndrivenSystemExecutors(definition, nextRun)
        var nextState = WorkflowRuntimeState(nextRun, nextHandles)
        persist(project, definition, nextState)

        if (nextRun.status.isTerminal() || nextRun.status == WorkflowRunStatus.AwaitingHuman) return nextState

        val dispatched = engine.dispatchReadyTasks(
            project = project,
            definition = definition,
            run = nextRun,
            existingHandles = nextHandles,
            nowEpochMillis = nowEpochMillis,
        )
        nextRun = dispatched.run
        nextHandles = dispatched.handles.filterKeys { taskId ->
            nextRun.taskRuns[taskId]?.status?.isActiveProviderStatus() == true
        }
        nextState = WorkflowRuntimeState(nextRun, nextHandles)
        persist(project, definition, nextState)
        return nextState
    }

    private fun preserveArtifactTimestamps(
        previous: WorkflowRun,
        reconciled: WorkflowRun,
    ): WorkflowRun {
        val previousArtifacts = previous.taskRuns.values
            .flatMap(TaskRun::artifacts)
            .associateBy { it.id }
        if (previousArtifacts.isEmpty()) return reconciled

        return reconciled.copy(
            taskRuns = reconciled.taskRuns.mapValues { (_, taskRun) ->
                taskRun.copy(
                    artifacts = taskRun.artifacts.map { artifact ->
                        previousArtifacts[artifact.id]?.let { old ->
                            artifact.copy(createdAtEpochMillis = old.createdAtEpochMillis)
                        } ?: artifact
                    },
                )
            },
        )
    }

    private fun blockUndrivenSystemExecutors(
        definition: WorkflowDefinition,
        run: WorkflowRun,
    ): WorkflowRun {
        val definitions = definition.tasks.associateBy { it.id }
        var changed = false
        val taskRuns = run.taskRuns.mapValues { (taskId, taskRun) ->
            val executor = taskRun.executor ?: definitions[taskId]?.executor
            val dispatchable = taskRun.status == TaskRunStatus.Ready ||
                taskRun.status == TaskRunStatus.Retrying ||
                taskRun.status == TaskRunStatus.Running
            if (dispatchable && executor.isUndrivenSystemExecutor()) {
                changed = true
                taskRun.copy(
                    status = TaskRunStatus.Blocked,
                    blockingReason = BlockingReason(
                        code = WorkflowRunFactory.EXECUTOR_INTEGRATION_UNAVAILABLE,
                        message = "${executor.displayLabel()} is not wired into the live runtime yet.",
                    ),
                    progress = null,
                    progressMessage = null,
                )
            } else {
                taskRun
            }
        }
        return if (changed) run.copy(taskRuns = taskRuns) else run
    }

    private fun TaskExecutor?.isUndrivenSystemExecutor(): Boolean = when (this) {
        is TaskExecutor.GitHubAction,
        is TaskExecutor.TestRunner,
        is TaskExecutor.Deployment,
        is TaskExecutor.RepositoryOperation,
        is TaskExecutor.ExternalService,
        is TaskExecutor.NestedWorkflow,
        -> true
        else -> false
    }

    private fun TaskExecutor?.displayLabel(): String = when (this) {
        is TaskExecutor.GitHubAction -> "GitHub Action executor"
        is TaskExecutor.TestRunner -> "Test runner executor"
        is TaskExecutor.Deployment -> "Deployment executor"
        is TaskExecutor.RepositoryOperation -> "Repository operation executor"
        is TaskExecutor.ExternalService -> "External service executor"
        is TaskExecutor.NestedWorkflow -> "Nested workflow executor"
        else -> "System executor"
    }

    private fun TaskRunStatus.isActiveProviderStatus(): Boolean = when (this) {
        TaskRunStatus.Planning,
        TaskRunStatus.AwaitingApproval,
        TaskRunStatus.Running,
        TaskRunStatus.Verifying,
        -> true
        else -> false
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
