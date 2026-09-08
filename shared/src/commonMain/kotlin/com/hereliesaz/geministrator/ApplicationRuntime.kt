package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.persistence.RepositoryWorkflowEventSink
import com.hereliesaz.geministrator.persistence.SettingsWorkflowPersistence
import com.hereliesaz.geministrator.persistence.WorkflowPersistence
import com.hereliesaz.geministrator.providers.AgentProvider
import com.hereliesaz.geministrator.providers.ProviderArtifact
import com.hereliesaz.geministrator.workflow.AgentProviderRegistry
import com.hereliesaz.geministrator.workflow.ManagedSessionGateway
import com.hereliesaz.geministrator.workflow.ProviderBackedManagedSessionGateway
import com.hereliesaz.geministrator.workflow.WorkflowEngine
import com.hereliesaz.geministrator.workflow.WorkflowRuntimeCoordinator
import com.hereliesaz.geministrator.workflow.WorkflowRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val scope: CoroutineScope,
    private val roles: List<RoleDefinition>,
) {
    private data class Current(
        val project: Project,
        val definition: WorkflowDefinition,
        val state: WorkflowRuntimeState,
    )

    private var current: Current? = null
    private var cycleJob: Job? = null

    val state: StateFlow<ApplicationRuntimeState> = publisher.state

    suspend fun loadLatest() {
        publisher.publish(ApplicationRuntimeState.Loading)
        try {
            val project = persistence.projects.all().maxByOrNull(Project::updatedAtEpochMillis)
            if (project == null) {
                current = null
                publisher.publish(ApplicationRuntimeState.NoProject)
                return
            }

            val run = persistence.runs.byProject(project.id).maxByOrNull { it.updatedAtEpochMillis }
            if (run == null) {
                current = null
                publisher.publish(ApplicationRuntimeState.NoRun(project))
                return
            }

            val definition = persistence.definitions.get(run.workflowDefinitionId)
                ?: error("Workflow definition ${run.workflowDefinitionId.value} was not found")
            val runtimeState = coordinator.resume(run.id)
            current = Current(project, definition, runtimeState)
            publishCurrent()
            startCycling()
        } catch (failure: Throwable) {
            current = null
            publishFailure(failure)
        }
    }

    suspend fun refresh() = loadLatest()

    private fun startCycling() {
        if (cycleJob?.isActive == true) return
        cycleJob = scope.launch {
            while (isActive) {
                delay(CYCLE_INTERVAL_MILLIS)
                val snapshot = current ?: continue
                if (snapshot.state.run.status.isTerminal()) continue
                try {
                    val nextState = coordinator.cycle(
                        project = snapshot.project,
                        definition = snapshot.definition,
                        state = snapshot.state,
                        nowEpochMillis = nowEpochMillis(),
                        artifactIdFactory = ::artifactId,
                    )
                    current = snapshot.copy(state = nextState)
                    publishCurrent()
                } catch (failure: Throwable) {
                    publishFailure(failure)
                    return@launch
                }
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

    private fun publishFailure(failure: Throwable) {
        val message = failure.message?.takeIf(String::isNotBlank) ?: failure::class.simpleName.orEmpty().ifBlank { "Runtime failure" }
        val disconnected = message.contains("provider", ignoreCase = true) &&
            (message.contains("registered", ignoreCase = true) || message.contains("connect", ignoreCase = true))
        publisher.publish(
            if (disconnected) ApplicationRuntimeState.Disconnected(message)
            else ApplicationRuntimeState.ResumeFailed(message),
        )
    }

    private fun WorkflowRunStatus.isTerminal(): Boolean = when (this) {
        WorkflowRunStatus.Completed,
        WorkflowRunStatus.Failed,
        WorkflowRunStatus.Cancelled,
        -> true
        else -> false
    }

    private fun artifactId(taskRun: TaskRun, artifact: ProviderArtifact, index: Int): ArtifactId =
        ArtifactId("${taskRun.id.value}:${artifact.kind}:$index")

    companion object {
        private const val CYCLE_INTERVAL_MILLIS = 1_000L

        suspend fun create(
            providers: Collection<AgentProvider>,
            scope: CoroutineScope,
            persistence: WorkflowPersistence = SettingsWorkflowPersistence.createDefault(),
        ): ApplicationRuntime {
            val storedRoles = persistence.roles.all()
            val roles = (BuiltInRoles.all + storedRoles).associateBy(RoleDefinition::id).values.toList()
            val registry = AgentProviderRegistry(providers)
            val gateway = ProviderBackedManagedSessionGateway(registry, scope)
            val publisher = WorkflowRuntimePublisher()
            val engine = WorkflowEngine(
                sessionGateway = gateway,
                roles = roles,
                eventSink = RepositoryWorkflowEventSink(persistence.events),
            )
            val coordinator = WorkflowRuntimeCoordinator(
                persistence = persistence,
                engine = engine,
                sessionGateway = gateway,
            )
            return ApplicationRuntime(
                persistence = persistence,
                providerRegistry = registry,
                sessionGateway = gateway,
                engine = engine,
                coordinator = coordinator,
                publisher = publisher,
                scope = scope,
                roles = roles,
            ).also { it.loadLatest() }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
