# Geministrator 2 Architecture

Status: implementation blueprint  
Branch: `architecture/geministrator-2`  
Salvage source of truth: `SALVAGE_MANIFEST.md`

## Product definition

Geministrator is a Compose Multiplatform control room for running governed software-development workflows across interchangeable agentic providers.

It targets:

- Android
- Desktop
- Web

The web application is a first-class product target, not a later port.

Geministrator is not an IDE, code editor, terminal, previewer, or file explorer. IDEaz owns those responsibilities.

Geministrator's job is to:

1. Turn a high-level objective into a governed workflow.
2. Represent that workflow as an explicit dependency graph.
3. Assign work to role-based agents.
4. Dispatch those agents through provider adapters, beginning with Google Jules.
5. Observe progress as typed events.
6. Enforce approval, verification, retry, escalation, and integration policies.
7. Surface only meaningful human decisions.
8. Preserve a complete, resumable audit trail.
9. Present the same supervisory experience across Android, Desktop, and Web.

The core product idea is:

> Geministrator is not an agent. It is the company that hires agents.

---

# 1. Architectural rules

1. Roles are not providers.
2. Providers do not leak into the workflow domain.
3. Tasks declare dependencies explicitly.
4. Parallelism is derived from the DAG, not prose.
5. A worker does not certify its own completion when independent verification is available.
6. Every meaningful state transition emits a typed event.
7. Every workflow run is resumable from persisted state.
8. Human intervention occurs only through explicit gates.
9. Provider failure must not corrupt workflow state.
10. Historical material under `salvage/` remains immutable.
11. Editor, terminal, preview, and file-explorer features do not grow back into this product.
12. Shared code must not depend on Android-only APIs.
13. Web is a first-class runtime constraint: filesystem, process execution, secure storage, and OAuth behavior must be abstracted behind platform interfaces.
14. Provider credentials, IDs, request payloads, and API-specific concepts remain behind provider/integration boundaries.

---

# 2. Compose Multiplatform structure

Target structure:

```text
Geministrator/
├── composeApp/
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── desktopMain/
│       └── webMain/
│
├── core/
│   ├── model/
│   ├── orchestration/
│   ├── workflow/
│   ├── events/
│   ├── roles/
│   └── policies/
│
├── data/
│   ├── repositories/
│   ├── persistence/
│   └── settings/
│
├── providers/
│   ├── api/
│   ├── jules/
│   └── a2a/
│
├── integrations/
│   └── github/
│
└── features/
    ├── projects/
    ├── workflows/
    ├── company/
    ├── runs/
    ├── inbox/
    └── settings/
```

The exact Gradle source-set naming may vary with the current Compose Multiplatform web target, but the architectural boundary is fixed: UI/domain lives in shared code; platform-only services live in platform source sets.

## Platform responsibilities

### commonMain

Owns:

- domain models
- workflow engine
- role definitions
- provider interfaces
- repositories interfaces
- feature state/ViewModels
- Compose UI
- navigation model
- validation
- policies
- event rendering

### androidMain

Owns only platform-specific implementations such as:

- encrypted/local credential storage
- Android lifecycle integration
- share/open-intent behavior
- notification integration if later needed

### desktopMain

Owns only platform-specific implementations such as:

- desktop secure storage
- native window behavior
- desktop URI opening

### webMain

Owns only browser-specific implementations such as:

- browser credential/session storage policy
- OAuth redirect handling
- browser URI/navigation behavior
- IndexedDB-backed persistence if selected
- browser visibility/reconnection behavior

The web build must not require a local filesystem or shell.

---

# 3. Domain model

The domain layer contains no Android, Compose, Jules, GitHub REST, browser, filesystem, or database types.

## Project

```kotlin
data class Project(
    val id: ProjectId,
    val name: String,
    val repository: RepositoryRef?,
    val defaultWorkflowTemplateId: WorkflowTemplateId?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

`RepositoryRef` contains provider-neutral repository identity only.

## WorkflowDefinition

Immutable definition of a workflow DAG.

```kotlin
data class WorkflowDefinition(
    val id: WorkflowDefinitionId,
    val name: String,
    val description: String?,
    val tasks: List<TaskDefinition>,
    val integrationPolicy: IntegrationPolicy,
    val concurrencyPolicy: ConcurrencyPolicy,
)
```

A workflow definition contains no execution state.

## TaskDefinition

```kotlin
data class TaskDefinition(
    val id: TaskDefinitionId,
    val name: String,
    val objective: String,
    val roleId: RoleDefinitionId,
    val dependsOn: Set<TaskDefinitionId>,
    val acceptanceCriteria: List<AcceptanceCriterion>,
    val requiredArtifacts: List<ArtifactRequirement>,
    val approvalPolicy: ApprovalPolicy,
    val verificationPolicy: VerificationPolicy,
    val retryPolicy: RetryPolicy,
    val escalationPolicy: EscalationPolicy,
    val providerConstraints: ProviderConstraints = ProviderConstraints.None,
)
```

Dependencies must form a DAG. Cycles are invalid at definition time.

## WorkflowRun

```kotlin
data class WorkflowRun(
    val id: WorkflowRunId,
    val projectId: ProjectId,
    val workflowDefinitionId: WorkflowDefinitionId,
    val objective: String,
    val status: WorkflowRunStatus,
    val taskRuns: Map<TaskDefinitionId, TaskRun>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

## TaskRun

```kotlin
data class TaskRun(
    val id: TaskRunId,
    val taskDefinitionId: TaskDefinitionId,
    val status: TaskRunStatus,
    val attempt: Int,
    val assignedRoleId: RoleDefinitionId,
    val assignedProviderId: AgentProviderId?,
    val providerRunId: ProviderRunId?,
    val artifacts: List<ArtifactRef>,
    val blockingReason: BlockingReason?,
)
```

## TaskRunStatus

```kotlin
sealed interface TaskRunStatus {
    data object Created : TaskRunStatus
    data object Blocked : TaskRunStatus
    data object Ready : TaskRunStatus
    data object Planning : TaskRunStatus
    data object AwaitingApproval : TaskRunStatus
    data object Running : TaskRunStatus
    data object Verifying : TaskRunStatus
    data object Retrying : TaskRunStatus
    data object Completed : TaskRunStatus
    data object Failed : TaskRunStatus
    data object Escalated : TaskRunStatus
    data object Cancelled : TaskRunStatus
}
```

---

# 4. Role system

Roles describe responsibility and authority. They are data, not hard-coded worker classes.

```kotlin
data class RoleDefinition(
    val id: RoleDefinitionId,
    val name: String,
    val description: String,
    val instructions: String,
    val enabled: Boolean,
    val preferredProviderId: AgentProviderId?,
    val capabilitiesRequired: Set<AgentCapability>,
    val authorities: Set<RoleAuthority>,
)
```

```kotlin
enum class RoleAuthority {
    PROPOSE_PLAN,
    REJECT_PLAN,
    APPROVE_PLAN,
    IMPLEMENT,
    VERIFY,
    REVIEW_CODE,
    APPROVE_INTEGRATION,
    APPROVE_RELEASE,
    DIAGNOSE_FAILURE,
}
```

Initial built-in roles:

1. Orchestrator
2. Product Manager
3. Researcher
4. Architect
5. UX Designer
6. Implementation Engineer
7. QA Engineer
8. Adversarial Reviewer
9. Code Reviewer
10. Recovery Engineer
11. Release Engineer

Users may edit, disable, duplicate, or add roles.

The original Antagonist role becomes the built-in Adversarial Reviewer and retains real authority to reject plans.

---

# 5. Workflow engine

`WorkflowEngine` owns orchestration state transitions. It does not perform coding work.

Responsibilities:

- validate workflow DAGs
- instantiate runs
- derive blocked and ready tasks
- enforce concurrency limits
- resolve role/provider assignment
- dispatch task requests
- consume provider events
- persist transitions
- enforce approval gates
- invoke verification
- schedule retries
- escalate failures
- coordinate integration
- resume interrupted runs

It must be an explicit state machine, not recursive orchestration.

## Ready rule

A task becomes `Ready` when:

1. every dependency is `Completed`, and
2. no unresolved gate blocks it.

If a dependency is `Failed`, `Escalated`, or `Cancelled`, dependents remain `Blocked` until a policy decision changes that condition.

## Concurrency

```kotlin
data class ConcurrencyPolicy(
    val maxConcurrentTasks: Int,
    val perProviderLimits: Map<AgentProviderId, Int> = emptyMap(),
)
```

The historical semaphore concept survives, but the scheduler applies it to ready task runs rather than raw coroutines.

---

# 6. Planning and authority

Planning exists at two levels.

## Workflow planning

The Orchestrator turns the user's objective into a `WorkflowDefinition` or adapts a saved template.

It determines:

- tasks
- dependencies
- roles
- acceptance criteria
- required artifacts
- gates
- verification

## Worker planning

A provider may return a plan for one task. That plan is a `TaskPlanArtifact`, not the workflow graph.

Example:

```text
Implementation Task
        ↓
Jules proposes plan
        ↓
Architect reviews
   ┌────┴────┐
approve    reject
   ↓          ↓
execute    objection → retry context
```

No worker may expand project scope merely because its provider can.

---

# 7. Provider abstraction

The workflow engine knows only `AgentProvider`.

```kotlin
interface AgentProvider {
    val id: AgentProviderId

    suspend fun capabilities(): AgentCapabilities

    suspend fun start(request: AgentTaskRequest): AgentRunHandle

    fun observe(runId: ProviderRunId): Flow<AgentEvent>

    suspend fun sendMessage(
        runId: ProviderRunId,
        message: String,
    )

    suspend fun approvePlan(runId: ProviderRunId): ProviderActionResult

    suspend fun cancel(runId: ProviderRunId): ProviderActionResult
}
```

## AgentTaskRequest

```kotlin
data class AgentTaskRequest(
    val taskRunId: TaskRunId,
    val objective: String,
    val roleInstructions: String,
    val acceptanceCriteria: List<AcceptanceCriterion>,
    val contextArtifacts: List<ArtifactRef>,
    val repository: RepositoryRef?,
    val isolationHint: IsolationHint,
    val requirePlanApproval: Boolean,
)
```

Provider tree:

```text
AgentProvider
├── JulesProvider
├── A2AProvider
├── CodexProvider        later
├── ClaudeProvider       later
└── CustomProvider       later
```

---

# 8. Jules provider

Jules is the first production provider.

Provider-internal mapping:

```text
RepositoryRef       → Jules Source
AgentTaskRequest    → Jules Session create request
ProviderRunId       → Jules Session ID
AgentEvent          ← Jules Activity
TaskPlanArtifact    ← plan-generated activity
PullRequestArtifact ← Jules output / PR result
approvePlan()       → Jules approvePlan endpoint
sendMessage()       → Jules sendMessage endpoint
```

Jules may use:

- source-backed sessions for repository work
- repoless sessions for research/planning where useful
- `requirePlanApproval = true` when Geministrator owns a plan gate
- automatic PR creation only when integration policy allows it

Jules source IDs, session IDs, activity payloads, and API keys stay in `providers/jules`.

## Web compatibility

Jules access must use normal HTTPS APIs from shared/provider code or a web-compatible transport implementation. The architecture must not depend on invoking Jules CLI binaries locally.

If browser security or secret-handling requirements later require a thin backend/BFF for production web deployments, that backend remains infrastructure for the provider adapter rather than becoming the workflow engine.

---

# 9. Artifacts

Agents communicate through artifacts, not implicit transcript inheritance.

```kotlin
sealed interface Artifact {
    val id: ArtifactId
    val taskRunId: TaskRunId
    val createdAt: Instant
}
```

Initial artifact types:

- RequirementArtifact
- ResearchArtifact
- ArchitectureArtifact
- DesignArtifact
- TaskPlanArtifact
- CodeChangeArtifact
- PullRequestArtifact
- TestResultArtifact
- ReviewArtifact
- VerificationArtifact
- FailureAnalysisArtifact
- ReleaseArtifact

A downstream task receives declared upstream artifacts plus explicit project context.

This prevents a workflow from degenerating into one giant shared chat transcript.

---

# 10. Verification and retries

Verification is independent from implementation whenever possible.

```kotlin
data class VerificationPolicy(
    val verifierRoleId: RoleDefinitionId?,
    val required: Boolean,
    val criteria: List<VerificationCriterion>,
)
```

A task may not become `Completed` until required verification passes.

## RetryPolicy

```kotlin
data class RetryPolicy(
    val maxAttempts: Int,
    val retryOn: Set<RetryReason>,
    val includeFailureContext: Boolean = true,
)
```

Retry context may contain:

- rejected-plan reasons
- failed acceptance criteria
- test output
- reviewer findings
- provider error summaries
- prior successful artifacts

Retries create new attempt records. They never overwrite the previous attempt.

---

# 11. Escalation and recovery

When retries are exhausted or a failure is non-retryable, the task becomes `Escalated`.

The Recovery Engineer may produce a `FailureAnalysisArtifact` containing:

- root cause
- affected tasks/artifacts
- recovery options
- recommended action
- risk level

The Orchestrator may then:

- retry with revised context
- replace provider/worker
- create a compensating task
- waive a non-critical failed requirement
- request human judgment
- fail the workflow

---

# 12. Approval gates and Inbox

Human attention is explicit.

```kotlin
data class ApprovalGate(
    val id: ApprovalGateId,
    val workflowRunId: WorkflowRunId,
    val taskRunId: TaskRunId?,
    val kind: ApprovalGateKind,
    val prompt: String,
    val options: List<ApprovalOption>,
    val status: ApprovalGateStatus,
)
```

Initial gate kinds:

- PLAN_APPROVAL
- SCOPE_CHANGE
- SECURITY_RISK
- INTEGRATION_APPROVAL
- RELEASE_APPROVAL
- FAILURE_ESCALATION

The Inbox shows unresolved gates only.

It must not become another chat inbox.

---

# 13. Integration

Integration is a workflow phase, not a side effect of implementation.

```kotlin
sealed interface IntegrationPolicy {
    data object Manual : IntegrationPolicy
    data object PullRequestOnly : IntegrationPolicy
    data class Automated(
        val requireCodeReview: Boolean,
        val requireQa: Boolean,
        val requireHumanApproval: Boolean,
    ) : IntegrationPolicy
}
```

For Jules-backed code work, Pull Requests are the preferred integration artifact.

Integration readiness is decided from concrete upstream artifacts, not a worker's completion message.

---

# 14. Event model

All state mutation produces an append-only `WorkflowEvent`.

```kotlin
data class WorkflowEvent(
    val id: WorkflowEventId,
    val workflowRunId: WorkflowRunId,
    val taskRunId: TaskRunId?,
    val sequence: Long,
    val timestamp: Instant,
    val type: WorkflowEventType,
    val payload: WorkflowEventPayload,
)
```

Initial event taxonomy:

```text
WORKFLOW_CREATED
WORKFLOW_PLANNING
WORKFLOW_STARTED
WORKFLOW_PAUSED
WORKFLOW_RESUMED
TASK_CREATED
TASK_BLOCKED
TASK_READY
ROLE_ASSIGNED
PROVIDER_ASSIGNED
AGENT_STARTED
PLAN_PROPOSED
PLAN_REJECTED
PLAN_APPROVED
HUMAN_DECISION_REQUIRED
HUMAN_DECISION_RECEIVED
TASK_PROGRESS
ARTIFACT_CREATED
PR_CREATED
TASK_COMPLETED
VERIFICATION_STARTED
VERIFICATION_FAILED
VERIFICATION_PASSED
RETRY_SCHEDULED
TASK_FAILED
TASK_ESCALATED
INTEGRATION_STARTED
INTEGRATION_FAILED
INTEGRATION_COMPLETED
WORKFLOW_FAILED
WORKFLOW_COMPLETED
WORKFLOW_CANCELLED
```

The event stream powers:

- persistence
- resumability
- audit history
- run timeline UI
- notifications
- debugging

---

# 15. Persistence

Persistence is repository-driven and platform-abstracted.

Required repositories:

```text
ProjectRepository
WorkflowDefinitionRepository
WorkflowRunRepository
WorkflowEventRepository
RoleRepository
ProviderConfigurationRepository
ApprovalGateRepository
ArtifactRepository
```

Shared code depends on interfaces only.

Potential implementations:

- Android/Desktop: local SQL database or compatible multiplatform persistence
- Web: IndexedDB-backed implementation or another browser-compatible store

The chosen persistence technology must support the three product targets before it is accepted.

Provider secrets are not stored in the general workflow database as plain text.

---

# 16. UI surfaces

The app is a supervisory console.

## Projects

Shows active projects and operational state.

```text
GRAFFITIXR
● 4 agents working
2 blocked
1 needs approval
Build 78% complete
```

## Workflow

Primary visualization of the DAG.

Must show:

- dependencies
- ready/running/blocked/completed tasks
- assigned role
- assigned provider
- gates
- verification status
- retries

## Company

Configures reusable roles.

Each role exposes:

- name
- responsibility
- standing instructions
- enabled state
- preferred provider
- required capabilities
- authority

## Run

Chronological event timeline.

```text
09:14 Architect produced implementation plan
09:15 Plan approved
09:15 Frontend and Backend launched in parallel
09:31 Backend produced PR #83
09:35 Frontend failed verification
09:35 QA returned task to Frontend
09:37 Frontend retry #2 started
```

## Inbox

Only unresolved human decision gates.

## Settings

Contains provider connections, provider credentials, concurrency defaults, role/template management, and platform-safe preferences.

## Responsive behavior

The shared Compose UI must support:

- narrow Android phone layouts
- tablet/desktop split panes
- resizable browser windows
- keyboard/mouse interaction on Desktop/Web
- touch interaction on Android/Web

No screen may assume a fixed mobile width.

---

# 17. Web product requirements

The Web target is part of the first architecture, not a packaging exercise.

Requirements:

1. No shared code may require `java.io.File`, `ProcessBuilder`, local Git, or shell commands.
2. Authentication flows must survive browser redirects/reloads.
3. Workflow runs remain recoverable after tab close/reopen.
4. Provider event observation must tolerate browser network suspension and reconnect cleanly.
5. Deep links must be able to address at least projects, workflow runs, and approval gates.
6. Sensitive provider credentials must not be casually persisted to ordinary browser storage.
7. Browser deployment must support static hosting where possible; any required backend must be narrowly scoped to security/provider transport needs.
8. UI must remain fully usable with mouse/keyboard and touch.

---

# 18. Initial workflow template

The first built-in template is `Android Feature Workflow`.

```text
Product Manager
       ↓
Architect
       ↓
Adversarial Reviewer
       ↓
 ┌─────┴─────────┐
 ↓               ↓
Implementation  Test Planner / QA Prep
 └─────┬─────────┘
       ↓
QA Engineer
       ↓
Code Reviewer
       ↓
Release Engineer
```

The DAG is configurable. This template merely proves the engine.

---

# 19. Migration from the current repository

The current IDE-era app is not the architectural base.

Migration strategy:

1. Preserve `salvage/` and `SALVAGE_MANIFEST.md` untouched.
2. Preserve reusable Jules API client logic where it cleanly maps to `JulesProvider`.
3. Preserve reusable GitHub API client logic behind `integrations/github`.
4. Remove IDE/editor/file-browser/terminal concepts from the new app surface.
5. Replace existing screen/navigation assumptions with the supervisory feature set.
6. Move domain logic into multiplatform shared modules/source sets.
7. Make Android, Desktop, and Web compile from the same shared domain/UI architecture.
8. Introduce provider interfaces before wiring Jules into the workflow engine.
9. Introduce typed event persistence before parallel execution.
10. Add workflow execution only after DAG validation, persistence, and event sequencing are deterministic.

---

# 20. Implementation sequence

## Milestone 1 — Multiplatform foundation

- Compose Multiplatform Android/Desktop/Web targets
- shared navigation shell
- shared theme
- platform service interfaces
- persistence abstraction

## Milestone 2 — Domain core

- IDs/value objects
- Project
- WorkflowDefinition
- TaskDefinition
- WorkflowRun
- TaskRun
- RoleDefinition
- policies
- DAG validation

## Milestone 3 — Event/persistence core

- WorkflowEvent model
- append-only event repository
- run snapshot persistence
- resumability

## Milestone 4 — Provider boundary

- AgentProvider
- provider capability model
- provider registry
- provider configuration

## Milestone 5 — Jules

- source discovery
- session creation
- activity observation
- plan approval
- messaging
- cancellation
- artifact/result mapping

## Milestone 6 — Workflow engine

- ready/blocked scheduler
- concurrency limits
- dispatch
- retries
- escalation
- approval gates

## Milestone 7 — Supervisory UI

- Projects
- Workflow DAG
- Company
- Run timeline
- Inbox
- Settings

## Milestone 8 — Integration/verification

- independent verification
- PR artifacts
- code review gate
- integration policy
- release role

## Milestone 9 — Templates and portability

- reusable workflow templates
- role templates
- import/export
- provider substitution

---

# 21. Definition of architectural success

The architecture is working when the following scenario requires no special-case provider logic in the workflow engine:

1. User opens the Web, Android, or Desktop app.
2. User selects a GitHub-backed project.
3. User starts an `Android Feature Workflow` with one objective.
4. Geministrator creates a DAG.
5. The Architect and Researcher run where dependencies permit.
6. An Adversarial Reviewer rejects a weak plan.
7. The implementation task retries with structured objection context.
8. Jules performs implementation and returns a PR artifact.
9. QA independently verifies acceptance criteria.
10. Code Review evaluates the concrete PR artifact.
11. A release gate is created only if policy requires human approval.
12. The full run survives app/browser restart.
13. The same run can be inspected from another supported platform.
14. Replacing Jules with another future `AgentProvider` does not alter workflow-domain code.

At that point Geministrator has returned to its original purpose, but with the architecture the original idea was missing.
