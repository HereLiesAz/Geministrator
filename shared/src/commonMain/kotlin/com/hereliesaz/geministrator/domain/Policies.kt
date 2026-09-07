package com.hereliesaz.geministrator.domain

sealed interface ApprovalPolicy {
    data object None : ApprovalPolicy
    data class RoleApproval(val authority: RoleAuthority) : ApprovalPolicy
    data object HumanApproval : ApprovalPolicy
}

sealed interface VerificationPolicy {
    data object None : VerificationPolicy
    data class Required(
        val verifierRoleId: RoleDefinitionId,
        val criteria: List<String> = emptyList(),
    ) : VerificationPolicy
}

data class RetryPolicy(
    val maxAttempts: Int = 2,
    val retryOn: Set<RetryReason> = setOf(
        RetryReason.ProviderFailure,
        RetryReason.PlanRejected,
        RetryReason.VerificationFailed,
    ),
    val includeFailureContext: Boolean = true,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
    }
}

enum class RetryReason {
    ProviderFailure,
    PlanRejected,
    VerificationFailed,
    TestsFailed,
    IntegrationConflict,
}

sealed interface EscalationPolicy {
    data object FailWorkflow : EscalationPolicy
    data object RequireHumanDecision : EscalationPolicy
    data class Reassign(val roleId: RoleDefinitionId) : EscalationPolicy
}

sealed interface ProviderConstraints {
    data object None : ProviderConstraints
    data class RequireCapabilities(val capabilities: Set<AgentCapability>) : ProviderConstraints
    data class RequireProvider(val providerId: AgentProviderId) : ProviderConstraints
}

data class ConcurrencyPolicy(
    val maxConcurrentTasks: Int = 4,
    val perProviderLimits: Map<AgentProviderId, Int> = emptyMap(),
) {
    init {
        require(maxConcurrentTasks >= 1) { "maxConcurrentTasks must be at least 1" }
        require(perProviderLimits.values.all { it >= 1 }) { "provider limits must be at least 1" }
    }
}

enum class IntegrationPolicy {
    Manual,
    PullRequest,
    AutoMergeAfterVerification,
}
