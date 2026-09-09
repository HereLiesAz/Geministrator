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

class ApplicationRuntimeApprovalTest {
    @Test
    fun humanApprovalCompletesGatePersistsAndPublishes() = runBlocking {
        val persistence = InMemoryWorkflowPersistence()
        val project = Project(
            id = ProjectId("project"),
            name = "Project",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        val taskId = TaskDefinitionId("approve")
        val executor = TaskExecutor.HumanApproval("Approve release")
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("definition"),
            name = "Release",
            tasks = listOf(
                TaskDefinition(
                    id = taskId,
                    name = "Release approval",
                    objective = "Approve release",
                    roleId = null,
                    executor = executor,
                ),
            ),
        )
        val run = WorkflowRun(
            id = WorkflowRunId("run"),
            projectId = project.id,
            workflowDefinitionId = definition.id,
            objective = "Ship",
            status = WorkflowRunStatus.AwaitingHuman,
            taskRuns = mapOf(
                taskId to TaskRun(
                    id = TaskRunId("task-run"),
                    taskDefinitionId = taskId,
                    status = TaskRunStatus.AwaitingApproval,
                    assignedRoleId = null,
                    executor = executor,
                ),
            ),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
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

            runtime.approveTask(taskId)

            val live = assertIs<ApplicationRuntimeState.Live>(runtime.state.value)
            assertEquals(WorkflowRunStatus.Completed, live.presentation.run.status)
            assertEquals(TaskRunStatus.Completed, live.presentation.run.taskRuns.getValue(taskId).status)
            assertEquals(WorkflowRunStatus.Completed, persistence.runs.get(run.id)?.status)
        } finally {
            scope.cancel()
        }
    }
}
