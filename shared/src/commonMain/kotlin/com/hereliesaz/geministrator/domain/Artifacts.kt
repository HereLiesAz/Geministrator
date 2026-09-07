package com.hereliesaz.geministrator.domain

enum class ArtifactKind {
    Requirement,
    Research,
    Architecture,
    EnvironmentSpecification,
    Design,
    TaskPlan,
    CodeChange,
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
