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
        instructions = "Implement only the assigned task and satisfy declared acceptance criteria and immutable approved pre-code verification artifacts.",
        authorities = setOf(RoleAuthority.Implement),
        capabilitiesRequired = setOf(
            AgentCapability.RepositoryRead,
            AgentCapability.RepositoryWrite,
        ),
    )

    val CrashTestDummy = RoleDefinition(
        id = RoleDefinitionId("crash-test-dummy"),
        name = "Crash Test Dummy",
        description = "Designs tests before implementation and expands them after implementation without certifying results.",
        instructions = "In specification mode, work only from approved requirements, concepts, architecture, UX specifications, constraints, and acceptance criteria; do not inspect implementation code. Produce acceptance test plans, behavioral tests, contract tests, invariants, edge cases, and failure scenarios that define what correct implementation must satisfy. In implementation mode, work from approved code changes plus the pre-code test contract to add regression tests and implementation-specific coverage. You may author test code and test plans, but you must not approve implementation, certify results, or weaken an approved pre-code test to make code pass. Any proposed change to an approved pre-code test must be escalated for Product or Architect approval.",
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
        instructions = "Attempt to falsify completion claims using the approved specification, pre-code verification contract, declared acceptance criteria, and post-code tests.",
        authorities = setOf(RoleAuthority.Verify),
        capabilitiesRequired = setOf(AgentCapability.Testing),
    )

    val AdversarialReviewer = RoleDefinition(
        id = RoleDefinitionId("adversarial-reviewer"),
        name = "Adversarial Reviewer",
        description = "Challenges plans and pre-code verification contracts before execution.",
        instructions = "Act as a cynical principal engineer. Find missing steps, hidden assumptions, risk, inadequate verification, and gaps in the pre-code test contract. Approve only when objections are resolved.",
        authorities = setOf(RoleAuthority.RejectPlan, RoleAuthority.ApprovePlan),
    )

    val CodeReviewer = RoleDefinition(
        id = RoleDefinitionId("code-reviewer"),
        name = "Code Reviewer",
        description = "Reviews code changes independently from implementation.",
        instructions = "Review correctness, maintainability, side effects, and adherence to the approved plan and pre-code verification contract.",
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
