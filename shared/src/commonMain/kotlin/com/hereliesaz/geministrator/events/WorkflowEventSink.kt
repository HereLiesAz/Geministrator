package com.hereliesaz.geministrator.events

interface WorkflowEventSink {
    suspend fun append(event: WorkflowEvent)
}

object NoOpWorkflowEventSink : WorkflowEventSink {
    override suspend fun append(event: WorkflowEvent) = Unit
}

class InMemoryWorkflowEventSink : WorkflowEventSink {
    private val items = mutableListOf<WorkflowEvent>()

    override suspend fun append(event: WorkflowEvent) {
        items += event
    }

    fun snapshot(): List<WorkflowEvent> = items.toList()
}
