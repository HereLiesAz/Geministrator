package com.hereliesaz.geministrator.domain

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryRef(
    val owner: String,
    val name: String,
    val defaultBranch: String? = null,
)

@Serializable
data class Project(
    val id: ProjectId,
    val name: String,
    val repository: RepositoryRef? = null,
    val defaultWorkflowTemplateId: WorkflowTemplateId? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class AcceptanceCriterion(
    val description: String,
)

@Serializable
sealed interface TaskExecutor {
    @Serializable
    data class RoleAgent(val roleId: RoleDefinitionId) : TaskExecutor

    @Serializable
    data class GitHubAction(val workflow: String, val ref: String? = null) : TaskExecutor

    @Serializable
    data class TestRunner(val command: String? = null) : TaskExecutor

    @Serializable
    data class Deployment(val environment: String) : TaskExecutor

    @Serializable
    data class RepositoryOperation(val operation: String) : TaskExecutor

    @Serializable
    data class HumanApproval(val label: String = "Human approval") : TaskExecutor

    @Serializable
    data class ExternalService(val service: String, val operation: String? = null) : TaskExecutor

    @Serializable
    data class NestedWorkflow(val workflowDefinitionId: WorkflowDefinitionId) : TaskExecutor
}

fun TaskDefinition.effectiveExecutor(): TaskExecutor = executor
    ?: roleId?.let(TaskExecutor::RoleAgent)
    ?: error("Task ${id.value} has neither an executor nor a responsible role")

fun TaskExecutor.displayName(): String = when (this) {
    is TaskExecutor.RoleAgent -> "Agent"
    is TaskExecutor.GitHubAction -> "GitHub Action"
    is TaskExecutor.TestRunner -> "Test Runner"
    is TaskExecutor.Deployment -> "Deployment"
    is TaskExecutor.RepositoryOperation -> "Repository Operation"
    is TaskExecutor.HumanApproval -> label
    is TaskExecutor.ExternalService -> service
    is TaskExecutor.NestedWorkflow -> "Nested Workflow"
}

@Serializable
data class TaskDefinition(
    val id: TaskDefinitionId,
    val name: String,
    val objective: String,
    val roleId: RoleDefinitionId?,
    val executor: TaskExecutor? = null,
    val dependsOn: Set<TaskDefinitionId> = emptySet(),
    val acceptanceCriteria: List<AcceptanceCriterion> = emptyList(),
    val requiredArtifacts: Set<ArtifactKind> = emptySet(),
    val approvalPolicy: ApprovalPolicy = ApprovalPolicy.None,
    val verificationPolicy: VerificationPolicy = VerificationPolicy.None,
    val retryPolicy: RetryPolicy = RetryPolicy(),
    val escalationPolicy: EscalationPolicy = EscalationPolicy.FailWorkflow,
    val providerConstraints: ProviderConstraints = ProviderConstraints.None,
    val environmentPlanningPolicy: EnvironmentPlanningPolicy = EnvironmentPlanningPolicy.WhenProviderRequires,
)

@Serializable
data class WorkflowDefinition(
    val id: WorkflowDefinitionId,
    val name: String,
    val description: String? = null,
    val tasks: List<TaskDefinition>,
    val integrationPolicy: IntegrationPolicy = IntegrationPolicy.PullRequest,
    val concurrencyPolicy: ConcurrencyPolicy = ConcurrencyPolicy(),
    val testDesignPolicy: TestDesignPolicy = TestDesignPolicy.BeforeAndAfterImplementation,
    val promptReusePolicy: PromptReusePolicy = PromptReusePolicy.PreferCache,
)

@Serializable
enum class WorkflowRunStatus {
    Created,
    Running,
    AwaitingHuman,
    Completed,
    Failed,
    Cancelled,
}

@Serializable
enum class TaskRunStatus {
    Created,
    Blocked,
    Ready,
    Planning,
    AwaitingApproval,
    Running,
    Verifying,
    Retrying,
    Completed,
    Failed,
    Escalated,
    Cancelled,
}

@Serializable
data class BlockingReason(
    val code: String,
    val message: String,
)

@Serializable
data class TaskRun(
    val id: TaskRunId,
    val taskDefinitionId: TaskDefinitionId,
    val status: TaskRunStatus,
    val attempt: Int = 1,
    val assignedRoleId: RoleDefinitionId?,
    val executor: TaskExecutor? = null,
    val assignedProviderId: AgentProviderId? = null,
    val providerRunId: ProviderRunId? = null,
    val externalRunId: String? = null,
    val artifacts: List<ArtifactRef> = emptyList(),
    val blockingReason: BlockingReason? = null,
    val progress: Float? = null,
    val progressMessage: String? = null,
) {
    init {
        require(progress == null || progress in 0f..1f) {
            "Task progress must be normalized 0f..1f"
        }
    }
}

@Serializable
data class WorkflowRun(
    val id: WorkflowRunId,
    val projectId: ProjectId,
    val workflowDefinitionId: WorkflowDefinitionId,
    val objective: String,
    val status: WorkflowRunStatus,
    val taskRuns: Map<TaskDefinitionId, TaskRun>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
