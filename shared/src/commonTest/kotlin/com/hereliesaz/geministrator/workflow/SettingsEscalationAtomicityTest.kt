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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
        val fixture = escalationFixture()
        persistence.runs.put(fixture.run)
        persistence.approvalGates.put(fixture.gate)

        val nextRun = fixture.approvedRun()
        val event = fixture.event(approved = true, at = 20L)

        assertTrue(
            persistence.commitFailureEscalationDecision(
                FailureEscalationDecisionCommit(
                    expectedGateId = fixture.gate.id,
                    decidedGate = fixture.gate.approve(null, "Retry", 20L),
                    nextRun = nextRun,
                    decisionEvent = event,
                ),
            ),
        )

        val restored = SettingsWorkflowPersistence(settings)
        assertEquals(ApprovalGateStatus.Approved, restored.approvalGates.get(fixture.gate.id)?.status)
        assertEquals(nextRun, restored.runs.get(fixture.run.id))
        assertEquals(listOf(event), restored.events.forRun(fixture.run.id).filterIsInstance<ApprovalDecisionReceived>())

        assertFalse(
            restored.commitFailureEscalationDecision(
                FailureEscalationDecisionCommit(
                    expectedGateId = fixture.gate.id,
                    decidedGate = fixture.gate.reject(null, "Stop", 21L),
                    nextRun = fixture.rejectedRun(21L),
                    decisionEvent = fixture.event(approved = false, at = 21L),
                ),
            ),
        )
        assertEquals(1, restored.events.forRun(fixture.run.id).filterIsInstance<ApprovalDecisionReceived>().size)
    }

    @Test
    fun sharedSettingsInstancesHaveSingleEscalationDecisionWinner() = runBlocking {
        val settings = MapSettings()
        val first = SettingsWorkflowPersistence(settings)
        val second = SettingsWorkflowPersistence(settings)
        val fixture = escalationFixture()
        first.runs.put(fixture.run)
        first.approvalGates.put(fixture.gate)

        val outcomes = coroutineScope {
            listOf(
                async {
                    first.commitFailureEscalationDecision(
                        FailureEscalationDecisionCommit(
                            expectedGateId = fixture.gate.id,
                            decidedGate = fixture.gate.approve(null, "Retry", 20L),
                            nextRun = fixture.approvedRun(),
                            decisionEvent = fixture.event(approved = true, at = 20L),
                        ),
                    )
                },
                async {
                    second.commitFailureEscalationDecision(
                        FailureEscalationDecisionCommit(
                            expectedGateId = fixture.gate.id,
                            decidedGate = fixture.gate.reject(null, "Stop", 21L),
                            nextRun = fixture.rejectedRun(21L),
                            decisionEvent = fixture.event(approved = false, at = 21L),
                        ),
                    )
                },
            ).awaitAll()
        }

        assertEquals(1, outcomes.count { it })
        assertEquals(1, outcomes.count { !it })

        val restored = SettingsWorkflowPersistence(settings)
        val gate = restored.approvalGates.get(fixture.gate.id)
        val run = restored.runs.get(fixture.run.id)
        val decisions = restored.events.forRun(fixture.run.id).filterIsInstance<ApprovalDecisionReceived>()
        assertEquals(1, decisions.size)
        if (gate?.status == ApprovalGateStatus.Approved) {
            assertEquals(WorkflowRunStatus.Running, run?.status)
            assertTrue(decisions.single().approved)
        } else {
            assertEquals(ApprovalGateStatus.Rejected, gate?.status)
            assertEquals(WorkflowRunStatus.Failed, run?.status)
            assertFalse(decisions.single().approved)
        }
    }

    private fun escalationFixture(): EscalationFixture {
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
                    assignedRoleId = null,
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
        return EscalationFixture(taskId, run, gate)
    }

    private data class EscalationFixture(
        val taskId: TaskDefinitionId,
        val run: WorkflowRun,
        val gate: ApprovalGate,
    ) {
        fun approvedRun() = run.copy(
            status = WorkflowRunStatus.Running,
            taskRuns = run.taskRuns + (
                taskId to run.taskRuns.getValue(taskId).copy(
                    status = TaskRunStatus.Retrying,
                    attempt = 2,
                )
            ),
            updatedAtEpochMillis = 20L,
        )

        fun rejectedRun(at: Long) = run.copy(
            status = WorkflowRunStatus.Failed,
            taskRuns = run.taskRuns + (
                taskId to run.taskRuns.getValue(taskId).copy(status = TaskRunStatus.Cancelled)
            ),
            updatedAtEpochMillis = at,
        )

        fun event(approved: Boolean, at: Long) = ApprovalDecisionReceived(
            workflowRunId = run.id,
            taskDefinitionId = taskId,
            gateId = gate.id,
            approved = approved,
            decidedByRoleId = null,
            occurredAtEpochMillis = at,
        )
    }
}
