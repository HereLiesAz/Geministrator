package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.ProjectId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.TaskRun
import com.hereliesaz.geministrator.domain.TaskRunId
import com.hereliesaz.geministrator.domain.TaskRunStatus
import com.hereliesaz.geministrator.domain.WorkflowDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRun
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.domain.WorkflowRunStatus
import com.hereliesaz.geministrator.events.ApprovalDecisionReceived
import com.hereliesaz.geministrator.persistence.SettingsWorkflowPersistence
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsEscalationAtomicityTest {
    @Test
    fun gateRunAndDecisionEventSurviveRecreationAsOneCommittedState() = runBlocking {
        val settings = MapSettings()
        val persistence = SettingsWorkflowPersistence(settings)
        val taskId = TaskDefinitionId("task")
        val run = WorkflowRun(
            id = WorkflowRunId("run"),
            projectId = ProjectId("project"),
            workflowDefinitionId = WorkflowDefinitionId("definition"),
            objective = "Recover",
            status = WorkflowRunStatus.AwaitingHuman,
            taskRuns = mapOf(
                taskId to TaskRun(
                    id = TaskRunId("task-run"),
                    taskDefinitionId = taskId,
                    status = TaskRunStatus.Escalated,
                ),
            ),
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 10L,
        )
        val gate = ApprovalGate(
            id = ApprovalGateId("failure:run:task:1"),
            workflowRunId = run.id,
            taskDefinitionId = taskId,
            kind = ApprovalGateKind.FailureEscalation,
            reason = "Executor failed",
            createdAtEpochMillis = 10L,
        )
        persistence.runs.put(run)
        persistence.approvalGates.put(gate)

        val nextRun = run.copy(
            status = WorkflowRunStatus.Running,
            taskRuns = run.taskRuns + (
                taskId to run.taskRuns.getValue(taskId).copy(
                    status = TaskRunStatus.Retrying,
                    attempt = 2,
                )
            ),
            updatedAtEpochMillis = 20L,
        )
        val decidedGate = gate.approve(null, "Retry", 20L)
        val event = ApprovalDecisionReceived(
            workflowRunId = run.id,
            taskDefinitionId = taskId,
            gateId = gate.id,
            approved = true,
            decidedByRoleId = null,
            occurredAtEpochMillis = 20L,
        )

        assertTrue(
            persistence.commitFailureEscalationDecision(
                FailureEscalationDecisionCommit(
                    expectedGateId = gate.id,
                    decidedGate = decidedGate,
                    nextRun = nextRun,
                    decisionEvent = event,
                ),
            ),
        )

        val restored = SettingsWorkflowPersistence(settings)
        assertEquals(ApprovalGateStatus.Approved, restored.approvalGates.get(gate.id)?.status)
        assertEquals(nextRun, restored.runs.get(run.id))
        assertEquals(listOf(event), restored.events.forRun(run.id).filterIsInstance<ApprovalDecisionReceived>())

        assertFalse(
            restored.commitFailureEscalationDecision(
                FailureEscalationDecisionCommit(
                    expectedGateId = gate.id,
                    decidedGate = gate.reject(null, "Stop", 21L),
                    nextRun = run.copy(status = WorkflowRunStatus.Failed, updatedAtEpochMillis = 21L),
                    decisionEvent = event.copy(approved = false, occurredAtEpochMillis = 21L),
                ),
            ),
        )
        assertEquals(1, restored.events.forRun(run.id).filterIsInstance<ApprovalDecisionReceived>().size)
    }
}
