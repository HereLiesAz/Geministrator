package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.BuiltInRoles
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.TaskDefinition
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskExecutor
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinition
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MixedExecutorWorkflowTest {
    @Test
    fun mixedAgentActionApprovalAndDeploymentShareOneDagWithoutFakeRoles() {
        val implement = TaskDefinitionId("implement")
        val verify = TaskDefinitionId("verify")
        val approve = TaskDefinitionId("approve")
        val deploy = TaskDefinitionId("deploy")

        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("mixed"),
            name = "Mixed executor workflow",
            tasks = listOf(
                TaskDefinition(
                    id = implement,
                    name = "Implement",
                    objective = "Implement the change",
                    roleId = BuiltInRoles.ImplementationEngineer.id,
                    executor = TaskExecutor.RoleAgent(BuiltInRoles.ImplementationEngineer.id),
                ),
                TaskDefinition(
                    id = verify,
                    name = "CI",
                    objective = "Run repository CI",
                    roleId = null,
                    executor = TaskExecutor.GitHubAction("ci.yml"),
                    dependsOn = setOf(implement),
                ),
                TaskDefinition(
                    id = approve,
                    name = "Release approval",
                    objective = "Approve release evidence",
                    roleId = BuiltInRoles.ReleaseEngineer.id,
                    executor = TaskExecutor.HumanApproval("Approve release"),
                    dependsOn = setOf(verify),
                ),
                TaskDefinition(
                    id = deploy,
                    name = "Deploy",
                    objective = "Deploy the verified build",
                    roleId = null,
                    executor = TaskExecutor.Deployment("production"),
                    dependsOn = setOf(approve),
                ),
            ),
        )

        assertTrue(WorkflowGraphValidator.validate(definition).isEmpty())

        val run = WorkflowRunFactory.create(
            definition = definition,
            workflowRunId = WorkflowRunId("run"),
            projectId = ProjectId("project"),
            objective = "Ship",
            nowEpochMillis = 1L,
            taskRunIdFactory = { TaskRunId("run-${it.value}") },
        )

        assertIs<TaskExecutor.RoleAgent>(run.taskRuns.getValue(implement).executor)
        assertIs<TaskExecutor.GitHubAction>(run.taskRuns.getValue(verify).executor)
        assertIs<TaskExecutor.HumanApproval>(run.taskRuns.getValue(approve).executor)
        assertIs<TaskExecutor.Deployment>(run.taskRuns.getValue(deploy).executor)
        assertEquals(null, run.taskRuns.getValue(verify).assignedRoleId)
        assertEquals(null, run.taskRuns.getValue(deploy).assignedRoleId)
        assertEquals(TaskRunStatus.Ready, run.taskRuns.getValue(implement).status)
        assertEquals(TaskRunStatus.Blocked, run.taskRuns.getValue(verify).status)
    }

    @Test
    fun validatorRejectsTaskWithNeitherResponsibilityNorExecutor() {
        val definition = WorkflowDefinition(
            id = WorkflowDefinitionId("invalid"),
            name = "Invalid",
            tasks = listOf(
                TaskDefinition(
                    id = TaskDefinitionId("orphan"),
                    name = "Orphan",
                    objective = "Cannot execute",
                    roleId = null,
                ),
            ),
        )

        assertIs<WorkflowValidationError.MissingExecutor>(WorkflowGraphValidator.validate(definition).single())
    }
}
