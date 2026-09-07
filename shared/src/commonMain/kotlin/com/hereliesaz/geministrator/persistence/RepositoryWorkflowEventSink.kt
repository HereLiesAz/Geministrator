package com.hereliesaz.geministrator.persistence

import com.hereliesaz.geministrator.events.WorkflowEvent
import com.hereliesaz.geministrator.events.WorkflowEventSink

class RepositoryWorkflowEventSink(
    private val repository: WorkflowEventRepository,
) : WorkflowEventSink {
    override suspend fun append(event: WorkflowEvent) {
        repository.append(event)
    }
}
