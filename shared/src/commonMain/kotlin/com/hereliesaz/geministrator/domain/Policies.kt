package com.hereliesaz.geministrator.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface ApprovalPolicy {
    @Serializable data object None : ApprovalPolicy
    @Serializable data class RoleApproval(val authority: RoleAuthority) : ApprovalPolicy
    @Serializable data object HumanApproval : ApprovalPolicy
}

@Serializable
sealed interface VerificationPolicy {
    @Serializable data object None : VerificationPolicy
    @Serializable data class Required(
        val verifierRoleId: RoleDefinitionId,
        val criteria: List<String> = emptyList(),
    ) : VerificationPolicy
}

@Serializable
enum class TestDesignPolicy {
    None,
    BeforeImplementation,
    AfterImplementation,
    BeforeAndAfterImplementation,
}

@Serializable
enum class PromptReusePolicy {
    ProviderDefault,
    PreferCache,
    DisableCache,
}

@Serializable
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

@Serializable
enum class RetryReason {
    ProviderFailure,
    PlanRejected,
    VerificationFailed,
    TestsFailed,
    IntegrationConflict,
}

@Serializable
sealed interface EscalationPolicy {
    @Serializable data object FailWorkflow : EscalationPolicy
    @Serializable data object RequireHumanDecision : EscalationPolicy
    @Serializable data class Reassign(val roleId: RoleDefinitionId) : EscalationPolicy
}

@Serializable
sealed interface ProviderConstraints {
    @Serializable data object None : ProviderConstraints
    @Serializable data class RequireCapabilities(val capabilities: Set<AgentCapability>) : ProviderConstraints
    @Serializable data class RequireProvider(val providerId: AgentProviderId) : ProviderConstraints
}

@Serializable
data class ConcurrencyPolicy(
    val maxConcurrentTasks: Int = 4,
    val perProviderLimits: Map<AgentProviderId, Int> = emptyMap(),
) {
    init {
        require(maxConcurrentTasks >= 1) { "maxConcurrentTasks must be at least 1" }
        require(perProviderLimits.values.all { it >= 1 }) { "provider limits must be at least 1" }
    }
}

@Serializable
enum class IntegrationPolicy {
    Manual,
    PullRequest,
    AutoMergeAfterVerification,
}
