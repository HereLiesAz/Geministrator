package com.hereliesaz.geministrator.persistence

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.ArtifactId
import com.hereliesaz.geministrator.domain.ArtifactRef
import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.RoleDefinition
import com.hereliesaz.geministrator.domain.RoleDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.events.WorkflowEvent
import com.hereliesaz.geministrator.workflow.ApprovalGate
import com.hereliesaz.geministrator.workflow.ApprovalGateRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsWorkflowPersistence(
    private val settings: Settings,
    private val storageKey: String = DEFAULT_STORAGE_KEY,
    private val json: Json = defaultJson,
) : WorkflowPersistence {
    private val mutex = Mutex()

    override val projects: ProjectRepository = object : ProjectRepository {
        override suspend fun put(project: Project) = update { snapshot ->
            snapshot.copy(projects = snapshot.projects.upsert(project) { it.id == project.id })
        }

        override suspend fun get(id: ProjectId): Project? = read().projects.firstOrNull { it.id == id }
        override suspend fun all(): List<Project> = read().projects
    }

    override val definitions: WorkflowDefinitionRepository = object : WorkflowDefinitionRepository {
        override suspend fun put(definition: WorkflowDefinition) = update { snapshot ->
            snapshot.copy(definitions = snapshot.definitions.upsert(definition) { it.id == definition.id })
        }

        override suspend fun get(id: WorkflowDefinitionId): WorkflowDefinition? =
            read().definitions.firstOrNull { it.id == id }

        override suspend fun all(): List<WorkflowDefinition> = read().definitions
    }

    override val runs: WorkflowRunRepository = object : WorkflowRunRepository {
        override suspend fun put(run: WorkflowRun) = update { snapshot ->
            snapshot.copy(runs = snapshot.runs.upsert(run) { it.id == run.id })
        }

        override suspend fun get(id: WorkflowRunId): WorkflowRun? = read().runs.firstOrNull { it.id == id }

        override suspend fun byProject(projectId: ProjectId): List<WorkflowRun> =
            read().runs.filter { it.projectId == projectId }.sortedByDescending { it.updatedAtEpochMillis }
    }

    override val events: WorkflowEventRepository = object : WorkflowEventRepository {
        override suspend fun append(event: WorkflowEvent) = update { snapshot ->
            snapshot.copy(events = snapshot.events + event)
        }

        override suspend fun forRun(workflowRunId: WorkflowRunId): List<WorkflowEvent> =
            read().events.filter { it.workflowRunId == workflowRunId }.sortedBy { it.occurredAtEpochMillis }
    }

    override val roles: RoleRepository = object : RoleRepository {
        override suspend fun put(role: RoleDefinition) = update { snapshot ->
            snapshot.copy(roles = snapshot.roles.upsert(role) { it.id == role.id })
        }

        override suspend fun get(id: RoleDefinitionId): RoleDefinition? = read().roles.firstOrNull { it.id == id }
        override suspend fun all(): List<RoleDefinition> = read().roles
    }

    override val artifacts: ArtifactRepository = object : ArtifactRepository {
        override suspend fun put(artifact: ArtifactRef) = update { snapshot ->
            snapshot.copy(artifacts = snapshot.artifacts.upsert(artifact) { it.id == artifact.id })
        }

        override suspend fun get(id: ArtifactId): ArtifactRef? = read().artifacts.firstOrNull { it.id == id }

        override suspend fun forRun(run: WorkflowRun): List<ArtifactRef> {
            val taskRunIds = run.taskRuns.values.mapTo(mutableSetOf()) { it.id }
            return read().artifacts.filter { it.taskRunId in taskRunIds }.sortedBy { it.createdAtEpochMillis }
        }
    }

    override val approvalGates: ApprovalGateRepository = object : ApprovalGateRepository {
        override suspend fun put(gate: ApprovalGate) = update { snapshot ->
            snapshot.copy(approvalGates = snapshot.approvalGates.upsert(gate) { it.id == gate.id })
        }

        override suspend fun get(id: ApprovalGateId): ApprovalGate? =
            read().approvalGates.firstOrNull { it.id == id }

        override suspend fun unresolved(workflowRunId: WorkflowRunId): List<ApprovalGate> =
            read().approvalGates.filter {
                it.workflowRunId == workflowRunId &&
                    it.status == com.hereliesaz.geministrator.workflow.ApprovalGateStatus.Pending
            }.sortedBy { it.createdAtEpochMillis }
    }

    suspend fun clearWorkflowData() {
        mutex.withLock { settings.remove(storageKey) }
    }

    suspend fun snapshotVersion(): Int = read().version

    private suspend fun read(): PersistenceSnapshot = mutex.withLock { readUnlocked() }

    private suspend fun update(transform: (PersistenceSnapshot) -> PersistenceSnapshot) {
        mutex.withLock {
            val next = transform(readUnlocked()).copy(version = CURRENT_SCHEMA_VERSION)
            settings.putString(storageKey, json.encodeToString(PersistenceSnapshot.serializer(), next))
        }
    }

    private fun readUnlocked(): PersistenceSnapshot {
        val encoded = settings.getStringOrNull(storageKey) ?: return PersistenceSnapshot()
        val snapshot = json.decodeFromString(PersistenceSnapshot.serializer(), encoded)
        require(snapshot.version <= CURRENT_SCHEMA_VERSION) {
            "Persistence schema ${snapshot.version} is newer than supported schema $CURRENT_SCHEMA_VERSION"
        }
        return migrate(snapshot)
    }

    private fun migrate(snapshot: PersistenceSnapshot): PersistenceSnapshot {
        var migrated = snapshot
        if (migrated.version < 2) {
            migrated = migrated.copy(
                version = 2,
                definitions = migrated.definitions.map { definition ->
                    definition.copy(
                        tasks = definition.tasks.map { task ->
                            if (task.executor != null || task.roleId == null) task
                            else task.copy(executor = TaskExecutor.RoleAgent(task.roleId))
                        },
                    )
                },
                runs = migrated.runs.map { run ->
                    run.copy(
                        taskRuns = run.taskRuns.mapValues { (_, taskRun) ->
                            if (taskRun.executor != null || taskRun.assignedRoleId == null) taskRun
                            else taskRun.copy(executor = TaskExecutor.RoleAgent(taskRun.assignedRoleId))
                        },
                    )
                },
            )
        }
        return migrated
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 2
        const val DEFAULT_STORAGE_KEY: String = "geministrator.workflow.persistence.v1"

        val defaultJson: Json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            classDiscriminator = "type"
            allowStructuredMapKeys = true
        }

        fun createDefault(): SettingsWorkflowPersistence = SettingsWorkflowPersistence(Settings())
    }
}

@Serializable
private data class PersistenceSnapshot(
    val version: Int = SettingsWorkflowPersistence.CURRENT_SCHEMA_VERSION,
    val projects: List<Project> = emptyList(),
    val definitions: List<WorkflowDefinition> = emptyList(),
    val runs: List<WorkflowRun> = emptyList(),
    val events: List<WorkflowEvent> = emptyList(),
    val roles: List<RoleDefinition> = emptyList(),
    val artifacts: List<ArtifactRef> = emptyList(),
    val approvalGates: List<ApprovalGate> = emptyList(),
)

private inline fun <T> List<T>.upsert(value: T, matches: (T) -> Boolean): List<T> {
    val index = indexOfFirst(matches)
    if (index < 0) return this + value
    return toMutableList().also { it[index] = value }
}
