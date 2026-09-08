package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.domain.Project
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.persistence.InMemoryWorkflowPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplicationRuntimeTest {
    @Test
    fun emptyPersistencePublishesNoProjectInsteadOfDemoWorkflow() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val runtime = ApplicationRuntime.create(
                providers = emptyList(),
                scope = scope,
                persistence = InMemoryWorkflowPersistence(),
            )

            assertEquals(ApplicationRuntimeState.NoProject, runtime.state.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun completedPersistedRunPublishesLivePresentation() = runBlocking {
        val persistence = InMemoryWorkflowPersistence()
        val project = Project(
            id = ProjectId("project"),
            name = "Haive",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
        )
        val taskId = TaskDefinitionId("approval")
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("definition"),
            name = "Release",
            tasks = listOf(
                TaskDefinition(
                    id = taskId,
                    name = "Approve",
                    objective = "Approve release",
                    roleId = null,
                    executor = TaskExecutor.HumanApproval(),
                ),
            ),
        )
        val run = WorkflowRun(
            id = WorkflowRunId("run"),
            projectId = project.id,
            workflowDefinitionId = definition.id,
            objective = "Ship",
            status = WorkflowRunStatus.Completed,
            taskRuns = mapOf(
                taskId to TaskRun(
                    id = TaskRunId("task-run"),
                    taskDefinitionId = taskId,
                    status = TaskRunStatus.Completed,
                    assignedRoleId = null,
                    executor = TaskExecutor.HumanApproval(),
                    progress = 1f,
                ),
            ),
            createdAtEpochMillis = 3L,
            updatedAtEpochMillis = 4L,
        )
        persistence.projects.put(project)
        persistence.definitions.put(definition)
        persistence.runs.put(run)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val runtime = ApplicationRuntime.create(
                providers = emptyList(),
                scope = scope,
                persistence = persistence,
            )

            val live = assertIs<ApplicationRuntimeState.Live>(runtime.state.value)
            assertEquals(run.id, live.presentation.run.id)
            assertEquals(definition.id, live.presentation.definition.id)
        } finally {
            scope.cancel()
        }
    }
}
