package com.hereliesaz.geministrator.domain

import kotlinx.serialization.Serializable

@Serializable data class ProjectId(val value: String)
@Serializable data class WorkflowDefinitionId(val value: String)
@Serializable data class WorkflowRunId(val value: String)
@Serializable data class TaskDefinitionId(val value: String)
@Serializable data class TaskRunId(val value: String)
@Serializable data class RoleDefinitionId(val value: String)
@Serializable data class AgentProviderId(val value: String)
@Serializable data class ProviderRunId(val value: String)
@Serializable data class ArtifactId(val value: String)
@Serializable data class WorkflowTemplateId(val value: String)
@Serializable data class ApprovalGateId(val value: String)
