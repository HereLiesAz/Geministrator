package com.hereliesaz.geministrator.events

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface WorkflowEventSink {
    suspend fun append(event: WorkflowEvent)
}

object NoOpWorkflowEventSink : WorkflowEventSink {
    override suspend fun append(event: WorkflowEvent) = Unit
}

class InMemoryWorkflowEventSink : WorkflowEventSink {
    private val mutex = Mutex()
    private val items = mutableListOf<WorkflowEvent>()

    override suspend fun append(event: WorkflowEvent) {
        mutex.withLock {
            items += event
        }
    }

    suspend fun snapshot(): List<WorkflowEvent> = mutex.withLock {
        items.toList()
    }
}
