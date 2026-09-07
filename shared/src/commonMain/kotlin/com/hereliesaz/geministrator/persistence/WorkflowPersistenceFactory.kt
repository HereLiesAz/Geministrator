package com.hereliesaz.geministrator.persistence

object WorkflowPersistenceFactory {
    fun createDurable(): WorkflowPersistence = SettingsWorkflowPersistence.createDefault()
}
