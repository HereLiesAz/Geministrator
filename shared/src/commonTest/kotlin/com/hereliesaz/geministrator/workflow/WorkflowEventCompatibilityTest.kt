package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.events.ApprovalRequired
import com.hereliesaz.geministrator.events.WorkflowEvent
import com.hereliesaz.geministrator.persistence.SettingsWorkflowPersistence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WorkflowEventCompatibilityTest {
    @Test
    fun currentApprovalRequiredAlwaysHasGateId() {
        val current = ApprovalRequired(
            workflowRunId = WorkflowRunId("run"),
            taskDefinitionId = TaskDefinitionId("task"),
            gateId = ApprovalGateId("gate"),
            reason = "Review",
            occurredAtEpochMillis = 10L,
        )

        assertTrue(current.hasGateId)
        assertEquals(ApprovalGateId("gate"), current.gateId)
    }

    @Test
    fun legacyApprovalRequiredWithoutGateIdStillDeserializesButCannotMasqueradeAsIdentified() {
        val json = SettingsWorkflowPersistence.defaultJson
        val current = ApprovalRequired(
            workflowRunId = WorkflowRunId("run"),
            taskDefinitionId = TaskDefinitionId("task"),
            gateId = ApprovalGateId("gate"),
            reason = "Review",
            occurredAtEpochMillis = 10L,
        )
        val encoded = json.encodeToString(WorkflowEvent.serializer(), current)
        val legacy = encoded.replace(
            "\"gateId\":{\"value\":\"gate\"}",
            "\"gateId\":null",
        )
        assertNotEquals(encoded, legacy)

        val restored = json.decodeFromString(WorkflowEvent.serializer(), legacy) as ApprovalRequired

        assertFalse(restored.hasGateId)
        assertFailsWith<IllegalArgumentException> { restored.gateId }
    }
}
