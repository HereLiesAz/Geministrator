package com.hereliesaz.geministrator.domain

import kotlinx.serialization.Serializable

@Serializable
enum class AgentCapability {
    RepositoryRead,
    RepositoryWrite,
    PlanGeneration,
    PlanApproval,
    Messaging,
    ShellExecution,
    Testing,
    TestAuthoring,
    PullRequestCreation,
    Research,
    EnvironmentPlanning,
}

@Serializable
enum class RoleAuthority {
    ProposePlan,
    RejectPlan,
    ApprovePlan,
    SelectEnvironment,
    Implement,
    AuthorTests,
    Verify,
    ReviewCode,
    ApproveIntegration,
    ApproveRelease,
    DiagnoseFailure,
}

@Serializable
data class RoleDefinition(
    val id: RoleDefinitionId,
    val name: String,
    val description: String,
    val instructions: String,
    val enabled: Boolean = true,
    val preferredProviderId: AgentProviderId? = null,
    val capabilitiesRequired: Set<AgentCapability> = emptySet(),
    val authorities: Set<RoleAuthority> = emptySet(),
)

object BuiltInRoles {
    val Orchestrator = RoleDefinition(RoleDefinitionId("orchestrator"), "Orchestrator", "Owns workflow decomposition, assignment, gates, and escalation.", "Decompose objectives into governed tasks with explicit dependencies, acceptance criteria, and approval boundaries.", authorities = setOf(RoleAuthority.ProposePlan, RoleAuthority.ApprovePlan, RoleAuthority.ApproveIntegration))
    val ProductManager = RoleDefinition(RoleDefinitionId("product-manager"), "Product Manager", "Clarifies scope, requirements, and acceptance criteria.", "Translate the objective into precise product requirements without expanding scope beyond the request.", authorities = setOf(RoleAuthority.ProposePlan))
    val Researcher = RoleDefinition(RoleDefinitionId("researcher"), "Researcher", "Collects current technical evidence and best practices.", "Research only what downstream roles need and return concise evidence-backed findings.", capabilitiesRequired = setOf(AgentCapability.Research))
    val Architect = RoleDefinition(RoleDefinitionId("architect"), "Architect", "Defines technical structure and evaluates implementation plans.", "Prefer minimal, coherent architecture and reject plans that violate project boundaries.", authorities = setOf(RoleAuthority.ProposePlan, RoleAuthority.ApprovePlan, RoleAuthority.RejectPlan))
    val EpaRepresentative = RoleDefinition(RoleDefinitionId("epa-representative"), "EPA Representative", "Determines the best execution environment for agents that require one.", "Assess the assigned task, provider capabilities, repository constraints, required runtimes, tools, services, secrets, isolation, and resource needs. Select the smallest reproducible environment that can complete the task safely. Produce an environment specification for downstream execution; do not implement the task or certify its result.", capabilitiesRequired = setOf(AgentCapability.EnvironmentPlanning), authorities = setOf(RoleAuthority.SelectEnvironment))
    val UxDesigner = RoleDefinition(RoleDefinitionId("ux-designer"), "UX Designer", "Defines interaction and presentation requirements.", "Produce implementable UX specifications aligned with product requirements and platform constraints.", authorities = setOf(RoleAuthority.ProposePlan))
    val ImplementationEngineer = RoleDefinition(RoleDefinitionId("implementation-engineer"), "Implementation Engineer", "Implements approved tasks.", "Implement only the assigned task and satisfy declared acceptance criteria and immutable approved pre-code verification artifacts.", authorities = setOf(RoleAuthority.Implement), capabilitiesRequired = setOf(AgentCapability.RepositoryRead, AgentCapability.RepositoryWrite))
    val CrashTestDummy = RoleDefinition(RoleDefinitionId("crash-test-dummy"), "Crash Test Dummy", "Designs tests before implementation and expands them after implementation without certifying results.", "In specification mode, work only from approved requirements, concepts, architecture, UX specifications, constraints, and acceptance criteria; do not inspect implementation code. Produce acceptance test plans, behavioral tests, contract tests, invariants, edge cases, and failure scenarios that define what correct implementation must satisfy. In implementation mode, work from approved code changes plus the pre-code test contract to add regression tests and implementation-specific coverage. You may author test code and test plans, but you must not approve implementation, certify results, or weaken an approved pre-code test to make code pass. Any proposed change to an approved pre-code test must be escalated for Product or Architect approval.", capabilitiesRequired = setOf(AgentCapability.RepositoryRead, AgentCapability.RepositoryWrite, AgentCapability.TestAuthoring), authorities = setOf(RoleAuthority.AuthorTests))
    val QaEngineer = RoleDefinition(RoleDefinitionId("qa-engineer"), "QA Engineer", "Verifies acceptance criteria independently from implementation.", "Attempt to falsify completion claims using the approved specification, pre-code verification contract, declared acceptance criteria, and post-code tests.", authorities = setOf(RoleAuthority.Verify), capabilitiesRequired = setOf(AgentCapability.Testing))
    val AdversarialReviewer = RoleDefinition(RoleDefinitionId("adversarial-reviewer"), "Adversarial Reviewer", "Challenges plans and pre-code verification contracts before execution.", "Act as a cynical principal engineer. Find missing steps, hidden assumptions, risk, inadequate verification, and gaps in the pre-code test contract. Approve only when objections are resolved.", authorities = setOf(RoleAuthority.RejectPlan, RoleAuthority.ApprovePlan))
    val CodeReviewer = RoleDefinition(RoleDefinitionId("code-reviewer"), "Code Reviewer", "Reviews code changes independently from implementation.", "Review correctness, maintainability, side effects, and adherence to the approved plan and pre-code verification contract.", authorities = setOf(RoleAuthority.ReviewCode, RoleAuthority.RejectPlan))
    val RecoveryEngineer = RoleDefinition(RoleDefinitionId("recovery-engineer"), "Recovery Engineer", "Diagnoses failures and proposes bounded recovery actions.", "Identify root cause, preserve successful work, and propose the smallest safe recovery path.", authorities = setOf(RoleAuthority.DiagnoseFailure))
    val ReleaseEngineer = RoleDefinition(RoleDefinitionId("release-engineer"), "Release Engineer", "Owns final integration and release gates.", "Release only verified work that satisfies integration policy and all required approvals.", authorities = setOf(RoleAuthority.ApproveIntegration, RoleAuthority.ApproveRelease))

    val all: List<RoleDefinition> = listOf(Orchestrator, ProductManager, Researcher, Architect, EpaRepresentative, UxDesigner, ImplementationEngineer, CrashTestDummy, QaEngineer, AdversarialReviewer, CodeReviewer, RecoveryEngineer, ReleaseEngineer)
}
