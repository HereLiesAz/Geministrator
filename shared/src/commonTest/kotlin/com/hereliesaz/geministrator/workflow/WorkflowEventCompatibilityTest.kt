package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.ApprovalGateId
import com.hereliesaz.geministrator.domain.TaskDefinitionId
import com.hereliesaz.geministrator.domain.WorkflowRunId
import com.hereliesaz.geministrator.events.ApprovalRequired
import com.hereliesaz.geministrator.events.WorkflowEvent
import com.hereliesaz.geministrator.persistence.SettingsWorkflowPersistence
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class WorkflowEventCompatibilityTest {
    @Test
    fun legacyApprovalRequiredWithoutGateIdStillDeserializesButCannotMasqueradeAsIdentified() {
        val json = SettingsWorkflowPersistence.defaultJson
        val current = ApprovalRequired(
            workflowRunId = WorkflowRunId("run"),
            taskDefinitionId = TaskDefinitionId("task"),
            serializedGateId = ApprovalGateId("gate"),
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
