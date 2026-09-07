package com.hereliesaz.geministrator.domain

enum class ArtifactKind {
    Requirement,
    Research,
    Architecture,
    Design,
    EnvironmentSpecification,
    TaskPlan,
    Specification,
    AcceptanceTestPlan,
    BehavioralTest,
    ContractTest,
    FailureScenario,
    CodeChange,
    TestPlan,
    TestCode,
    RegressionTest,
    CommandOutput,
    Media,
    PullRequest,
    TestResult,
    Review,
    Verification,
    FailureAnalysis,
    Release,
}

data class ArtifactRef(
    val id: ArtifactId,
    val kind: ArtifactKind,
    val taskRunId: TaskRunId,
    val label: String,
    val uri: String? = null,
    val createdAtEpochMillis: Long,
)
