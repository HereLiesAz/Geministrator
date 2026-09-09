package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.domain.effectiveExecutor
import com.hereliesaz.geministrator.persistence.RepositoryWorkflowEventSink
import com.hereliesaz.geministrator.persistence.SettingsWorkflowPersistence
import com.hereliesaz.geministrator.persistence.WorkflowPersistence
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.ProviderActionResult
import com.hereliesaz.geministrator.providers.ProviderArtifact
import com.hereliesaz.geministrator.workflow.AgentProviderRegistry
import com.hereliesaz.geministrator.workflow.ManagedSessionFailure
import com.hereliesaz.geministrator.workflow.ManagedSessionGateway
import com.hereliesaz.geministrator.workflow.ManagedSessionStatus
import com.hereliesaz.geministrator.workflow.ProviderBackedManagedSessionGateway
import com.hereliesaz.geministrator.workflow.StarterWorkflowFactory
import com.hereliesaz.geministrator.workflow.TaskExecutorIntegrationRegistry
import com.hereliesaz.geministrator.workflow.WorkflowDefinitionPreparer
import com.hereliesaz.geministrator.workflow.WorkflowEngine
import com.hereliesaz.geministrator.workflow.WorkflowLaunchService
import com.hereliesaz.geministrator.workflow.WorkflowRuntimeCoordinator
import com.hereliesaz.geministrator.workflow.WorkflowRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface ApplicationRuntimeState {
    data object Loading : ApplicationRuntimeState
    data object NoProject : ApplicationRuntimeState
    data class NoRun(val project: Project) : ApplicationRuntimeState
    data class Live(val presentation: LiveWorkflowPresentation) : ApplicationRuntimeState
    data class Disconnected(val message: String) : ApplicationRuntimeState
    data class ResumeFailed(val message: String) : ApplicationRuntimeState
}

sealed class ApplicationRuntimeFailure(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class Disconnected(message: String, cause: Throwable? = null) : ApplicationRuntimeFailure(message, cause)
    class Resume(message: String, cause: Throwable? = null) : ApplicationRuntimeFailure(message, cause)
}

class WorkflowRuntimePublisher {
    private val mutableState = MutableStateFlow<ApplicationRuntimeState>(ApplicationRuntimeState.Loading)
    val state: StateFlow<ApplicationRuntimeState> = mutableState.asStateFlow()

    fun publish(state: ApplicationRuntimeState) {
        mutableState.value = state
    }
}

class ApplicationRuntime private constructor(
    val persistence: WorkflowPersistence,
    val providerRegistry: AgentProviderRegistry,
    val sessionGateway: ManagedSessionGateway,
    val engine: WorkflowEngine,
    val coordinator: WorkflowRuntimeCoordinator,
    val publisher: WorkflowRuntimePublisher,
    private val runtimeScope: CoroutineScope,
    private val roles: List<RoleDefinition>,
) {
    private data class Current(
        val project: Project,
        val definition: WorkflowDefinition,
        val state: WorkflowRuntimeState,
    )

    private var current: Current? = null
    private var currentGeneration: Long = 0L
    private var cycleJob: Job? = null
    private val runtimeMutex = Mutex()
    val state: StateFlow<ApplicationRuntimeState> = publisher.state

    suspend fun loadLatest() {
        runtimeMutex.withLock {
            publisher.publish(ApplicationRuntimeState.Loading)
            try {
                val projects = persistence.projects.all()
                if (projects.isEmpty()) {
                    replaceCurrent(null)
                    publisher.publish(ApplicationRuntimeState.NoProject)
                    return@withLock
                }

                val latestProjectRun = projects
                    .flatMap { project ->
                        persistence.runs.byProject(project.id).map { run -> project to run }
                    }
                    .maxByOrNull { (_, run) -> run.updatedAtEpochMillis }

                if (latestProjectRun == null) {
                    replaceCurrent(null)
                    publisher.publish(
                        ApplicationRuntimeState.NoRun(
                            projects.maxByOrNull(Project::updatedAtEpochMillis) ?: projects.first(),
                        ),
                    )
                    return@withLock
                }

                val (project, run) = latestProjectRun
                val definition = persistence.definitions.get(run.workflowDefinitionId)
                    ?: error("Workflow definition ${run.workflowDefinitionId.value} was not found")
                val runtimeState = try {
                    coordinator.resume(run.id)
                } catch (failure: Throwable) {
                    throw classifyResumeFailure(failure)
                }

                replaceCurrent(Current(project, definition, runtimeState))
                publishCurrent()
                startCycling()
            } catch (failure: Throwable) {
                replaceCurrent(null)
                publishFailure(failure)
            }
        }
    }

    suspend fun refresh() = loadLatest()

    suspend fun launchStarterWorkflow(
        projectName: String,
        objective: String,
        existingProject: Project? = null,
    ) {
        runtimeMutex.withLock {
            val cleanProjectName = projectName.trim()
            val cleanObjective = objective.trim()
            require(cleanProjectName.isNotEmpty()) { "Project name is required" }
            require(cleanObjective.isNotEmpty()) { "Objective is required" }

            val now = nowEpochMillis()
            val project = existingProject?.copy(
                name = cleanProjectName,
                updatedAtEpochMillis = now,
            ) ?: Project(
                id = ProjectId("project-$now"),
                name = cleanProjectName,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            )
            val definition = StarterWorkflowFactory.create(
                id = WorkflowDefinitionId("workflow-$now"),
                objective = cleanObjective,
            )
            val launchService = WorkflowLaunchService(
                preparer = WorkflowDefinitionPreparer(providerRegistry, roles),
                persistence = persistence,
                eventSink = RepositoryWorkflowEventSink(persistence.events),
                roles = roles,
            )
            val (prepared, runtimeState) = launchService.launch(
                project = project,
                definition = definition,
                workflowRunId = WorkflowRunId("run-$now"),
                objective = cleanObjective,
                nowEpochMillis = now,
                taskRunIdFactory = { id -> TaskRunId("run-$now-${id.value}") },
            )

            replaceCurrent(Current(project, prepared, runtimeState))
            publishCurrent()
            startCycling()
        }
    }

    suspend fun approveTask(taskDefinitionId: TaskDefinitionId) {
        runtimeMutex.withLock {
            val snapshot = requireNotNull(current) { "No active workflow is loaded" }
            val task = requireNotNull(snapshot.definition.tasks.firstOrNull { it.id == taskDefinitionId }) {
                "Task ${taskDefinitionId.value} is not defined"
            }
            val taskRun = requireNotNull(snapshot.state.run.taskRuns[taskDefinitionId]) {
                "Task ${taskDefinitionId.value} has no runtime state"
            }
            require(taskRun.status == TaskRunStatus.AwaitingApproval) {
                "Task ${taskDefinitionId.value} is not awaiting approval"
            }

            val now = nowEpochMillis()
            val nextState = if (taskRun.assignedProviderId != null) {
                val handle = requireNotNull(snapshot.state.handles[taskDefinitionId]) {
                    "Task ${taskDefinitionId.value} has no provider session to approve"
                }
                when (val result = sessionGateway.approvePlan(handle)) {
                    ProviderActionResult.Accepted -> {
                        if (sessionGateway.status(handle) == ManagedSessionStatus.Completed) {
                            WorkflowRuntimeState(
                                run = engine.completeTask(
                                    definition = snapshot.definition,
                                    run = snapshot.state.run,
                                    taskDefinitionId = taskDefinitionId,
                                    nowEpochMillis = now,
                                    artifacts = snapshot.state.run.taskRuns.getValue(taskDefinitionId).artifacts,
                                ),
                                handles = snapshot.state.handles,
                            )
                        } else {
                            val nextRun = snapshot.state.run.copy(
                                status = WorkflowRunStatus.Running,
                                taskRuns = snapshot.state.run.taskRuns + (
                                    taskDefinitionId to taskRun.copy(
                                        status = TaskRunStatus.Running,
                                        progressMessage = "Plan approved",
                                    )
                                ),
                                updatedAtEpochMillis = now,
                            )
                            WorkflowRuntimeState(nextRun, snapshot.state.handles)
                        }
                    }
                    is ProviderActionResult.Rejected -> error(result.reason)
                }
            } else {
                require(task.effectiveExecutor() is TaskExecutor.HumanApproval) {
                    "Task ${taskDefinitionId.value} is not a human approval gate"
                }
                WorkflowRuntimeState(
                    run = engine.completeHumanApprovalTask(
                        definition = snapshot.definition,
                        run = snapshot.state.run,
                        taskDefinitionId = taskDefinitionId,
                        nowEpochMillis = now,
                    ),
                    handles = snapshot.state.handles,
                )
            }

            coordinator.persist(snapshot.project, snapshot.definition, nextState)
            replaceCurrent(snapshot.copy(state = nextState))
            publishCurrent()
        }
    }

    fun close() {
        cycleJob?.cancel()
        cycleJob = null
        replaceCurrent(null)
        runtimeScope.cancel()
    }

    private fun replaceCurrent(next: Current?) {
        currentGeneration += 1L
        current = next
    }

    private fun startCycling() {
        if (cycleJob?.isActive == true) return
        cycleJob = runtimeScope.launch {
            while (isActive) {
                delay(CYCLE_INTERVAL_MILLIS)
                var failed = false
                runtimeMutex.withLock {
                    val snapshot = current ?: return@withLock
                    val snapshotGeneration = currentGeneration
                    if (snapshot.state.run.status.isTerminal()) return@withLock

                    try {
                        val nextState = coordinator.cycle(
                            project = snapshot.project,
                            definition = snapshot.definition,
                            state = snapshot.state,
                            nowEpochMillis = nowEpochMillis(),
                            artifactIdFactory = ::artifactId,
                        )
                        if (currentGeneration != snapshotGeneration || current !== snapshot) return@withLock
                        replaceCurrent(snapshot.copy(state = nextState))
                        publishCurrent()
                    } catch (failure: Throwable) {
                        if (currentGeneration == snapshotGeneration && current === snapshot) {
                            publishFailure(classifyRuntimeFailure(failure))
                        }
                        failed = true
                    }
                }
                if (failed) delay(RETRY_BACKOFF_MILLIS)
            }
        }
    }

    private fun publishCurrent() {
        val snapshot = current ?: return
        publisher.publish(
            ApplicationRuntimeState.Live(
                LiveWorkflowPresentation(
                    definition = snapshot.definition,
                    run = snapshot.state.run,
                    roles = roles,
                ),
            ),
        )
    }

    internal fun publishFailure(failure: Throwable) {
        val message = failure.message
            ?.takeIf(String::isNotBlank)
            ?: failure::class.simpleName.orEmpty().ifBlank { "Runtime failure" }
        publisher.publish(
            if (failure is ApplicationRuntimeFailure.Disconnected) {
                ApplicationRuntimeState.Disconnected(message)
            } else {
                ApplicationRuntimeState.ResumeFailed(message)
            },
        )
    }

    private fun WorkflowRunStatus.isTerminal() =
        this == WorkflowRunStatus.Completed ||
            this == WorkflowRunStatus.Failed ||
            this == WorkflowRunStatus.Cancelled

    private fun artifactId(
        taskRun: TaskRun,
        artifact: ProviderArtifact,
        index: Int,
    ) = ArtifactId("${taskRun.id.value}:${artifact.kind}:$index")

    companion object {
        private const val CYCLE_INTERVAL_MILLIS = 1_000L
        private const val RETRY_BACKOFF_MILLIS = 2_000L

        internal fun classifyResumeFailure(failure: Throwable): ApplicationRuntimeFailure =
            classifyRuntimeFailure(failure)

        internal fun classifyRuntimeFailure(failure: Throwable): ApplicationRuntimeFailure = when (failure) {
            is ApplicationRuntimeFailure -> failure
            is ManagedSessionFailure.ProviderUnavailable -> ApplicationRuntimeFailure.Disconnected(
                failure.message ?: "Provider runtime is unavailable",
                failure,
            )
            else -> ApplicationRuntimeFailure.Resume(
                failure.message ?: "Runtime operation failed",
                failure,
            )
        }

        suspend fun create(
            providers: Collection<AgentProvider>,
            scope: CoroutineScope,
            persistence: WorkflowPersistence = SettingsWorkflowPersistence.createDefault(),
            executorIntegrations: TaskExecutorIntegrationRegistry = TaskExecutorIntegrationRegistry.Empty,
        ): ApplicationRuntime {
            val runtimeJob = SupervisorJob(scope.coroutineContext[Job])
            val runtimeScope = CoroutineScope(scope.coroutineContext + runtimeJob)
            val registry = AgentProviderRegistry(providers)
            val gateway = ProviderBackedManagedSessionGateway(registry, runtimeScope)
            val publisher = WorkflowRuntimePublisher()

            fun build(roles: List<RoleDefinition>): ApplicationRuntime {
                val engine = WorkflowEngine(
                    sessionGateway = gateway,
                    roles = roles,
                    eventSink = RepositoryWorkflowEventSink(persistence.events),
                )
                val coordinator = WorkflowRuntimeCoordinator(
                    persistence = persistence,
                    engine = engine,
                    sessionGateway = gateway,
                    executorIntegrations = executorIntegrations,
                )
                return ApplicationRuntime(
                    persistence,
                    registry,
                    gateway,
                    engine,
                    coordinator,
                    publisher,
                    runtimeScope,
                    roles,
                )
            }

            val runtime = try {
                val roles = (BuiltInRoles.all + persistence.roles.all())
                    .associateBy(RoleDefinition::id)
                    .values
                    .toList()
                build(roles)
            } catch (failure: Throwable) {
                build(BuiltInRoles.all).also {
                    it.publishFailure(
                        ApplicationRuntimeFailure.Resume(
                            failure.message ?: "Runtime bootstrap failed",
                            failure,
                        ),
                    )
                }
            }

            if (runtime.state.value == ApplicationRuntimeState.Loading) {
                runtime.loadLatest()
            }
            return runtime
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
