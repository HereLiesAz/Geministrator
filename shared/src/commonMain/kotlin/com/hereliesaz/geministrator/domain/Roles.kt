package com.hereliesaz.geministrator.domain

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
    val Orchestrator = RoleDefinition(
        id = RoleDefinitionId("orchestrator"),
        name = "Orchestrator",
        description = "Owns workflow decomposition, assignment, gates, and escalation.",
        instructions = "Decompose objectives into governed tasks with explicit dependencies, acceptance criteria, and approval boundaries.",
        authorities = setOf(
            RoleAuthority.ProposePlan,
            RoleAuthority.ApprovePlan,
            RoleAuthority.ApproveIntegration,
        ),
    )

    val ProductManager = RoleDefinition(
        id = RoleDefinitionId("product-manager"),
        name = "Product Manager",
        description = "Clarifies scope, requirements, and acceptance criteria.",
        instructions = "Translate the objective into precise product requirements without expanding scope beyond the request.",
        authorities = setOf(RoleAuthority.ProposePlan),
    )

    val Researcher = RoleDefinition(
        id = RoleDefinitionId("researcher"),
        name = "Researcher",
        description = "Collects current technical evidence and best practices.",
        instructions = "Research only what downstream roles need and return concise evidence-backed findings.",
        capabilitiesRequired = setOf(AgentCapability.Research),
    )

    val Architect = RoleDefinition(
        id = RoleDefinitionId("architect"),
        name = "Architect",
        description = "Defines technical structure and evaluates implementation plans.",
        instructions = "Prefer minimal, coherent architecture and reject plans that violate project boundaries.",
        authorities = setOf(
            RoleAuthority.ProposePlan,
            RoleAuthority.ApprovePlan,
            RoleAuthority.RejectPlan,
        ),
    )

    val EpaRepresentative = RoleDefinition(
        id = RoleDefinitionId("epa-representative"),
        name = "EPA Representative",
        description = "Determines the best execution environment for agents that require one.",
        instructions = "Assess the assigned task, provider capabilities, repository constraints, required runtimes, tools, services, secrets, isolation, and resource needs. Select the smallest reproducible environment that can complete the task safely. Produce an environment specification for downstream execution; do not implement the task or certify its result.",
        capabilitiesRequired = setOf(AgentCapability.EnvironmentPlanning),
        authorities = setOf(RoleAuthority.SelectEnvironment),
    )

    val UxDesigner = RoleDefinition(
        id = RoleDefinitionId("ux-designer"),
        name = "UX Designer",
        description = "Defines interaction and presentation requirements.",
        instructions = "Produce implementable UX specifications aligned with product requirements and platform constraints.",
        authorities = setOf(RoleAuthority.ProposePlan),
    )

    val ImplementationEngineer = RoleDefinition(
        id = RoleDefinitionId("implementation-engineer"),
        name = "Implementation Engineer",
        description = "Implements approved tasks.",
        instructions = "Implement only the assigned task and satisfy declared acceptance criteria.",
        authorities = setOf(RoleAuthority.Implement),
        capabilitiesRequired = setOf(
            AgentCapability.RepositoryRead,
            AgentCapability.RepositoryWrite,
        ),
    )

    val CrashTestDummy = RoleDefinition(
        id = RoleDefinitionId("crash-test-dummy"),
        name = "Crash Test Dummy",
        description = "Produces tests for approved implementation artifacts.",
        instructions = "Work only from approved code or approved implementation artifacts. Derive high-value automated tests from acceptance criteria, architecture, known risks, and changed behavior. Prefer tests that expose regressions and edge cases. You may author test code and test plans, but you must not approve the implementation, certify the result, or weaken tests to make code pass.",
        capabilitiesRequired = setOf(
            AgentCapability.RepositoryRead,
            AgentCapability.RepositoryWrite,
            AgentCapability.TestAuthoring,
        ),
        authorities = setOf(RoleAuthority.AuthorTests),
    )

    val QaEngineer = RoleDefinition(
        id = RoleDefinitionId("qa-engineer"),
        name = "QA Engineer",
        description = "Verifies acceptance criteria independently from implementation.",
        instructions = "Attempt to falsify completion claims using the declared acceptance and verification criteria.",
        authorities = setOf(RoleAuthority.Verify),
        capabilitiesRequired = setOf(AgentCapability.Testing),
    )

    val AdversarialReviewer = RoleDefinition(
        id = RoleDefinitionId("adversarial-reviewer"),
        name = "Adversarial Reviewer",
        description = "Challenges plans before execution.",
        instructions = "Act as a cynical principal engineer. Find missing steps, hidden assumptions, risk, and inadequate verification. Approve only when objections are resolved.",
        authorities = setOf(RoleAuthority.RejectPlan, RoleAuthority.ApprovePlan),
    )

    val CodeReviewer = RoleDefinition(
        id = RoleDefinitionId("code-reviewer"),
        name = "Code Reviewer",
        description = "Reviews code changes independently from implementation.",
        instructions = "Review correctness, maintainability, side effects, and adherence to the approved plan.",
        authorities = setOf(RoleAuthority.ReviewCode, RoleAuthority.RejectPlan),
    )

    val RecoveryEngineer = RoleDefinition(
        id = RoleDefinitionId("recovery-engineer"),
        name = "Recovery Engineer",
        description = "Diagnoses failures and proposes bounded recovery actions.",
        instructions = "Identify root cause, preserve successful work, and propose the smallest safe recovery path.",
        authorities = setOf(RoleAuthority.DiagnoseFailure),
    )

    val ReleaseEngineer = RoleDefinition(
        id = RoleDefinitionId("release-engineer"),
        name = "Release Engineer",
        description = "Owns final integration and release gates.",
        instructions = "Release only verified work that satisfies integration policy and all required approvals.",
        authorities = setOf(RoleAuthority.ApproveIntegration, RoleAuthority.ApproveRelease),
    )

    val all: List<RoleDefinition> = listOf(
        Orchestrator,
        ProductManager,
        Researcher,
        Architect,
        EpaRepresentative,
        UxDesigner,
        ImplementationEngineer,
        CrashTestDummy,
        QaEngineer,
        AdversarialReviewer,
        CodeReviewer,
        RecoveryEngineer,
        ReleaseEngineer,
    )
}
