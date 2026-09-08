# Geministrator Salvage Manifest

Branch: `salvage/origins`

Purpose: classify historical Geministrator material before any modernization work. Historical snapshots under `salvage/` remain untouched; this file is the decision ledger.

Verdicts:
- **KEEP** — preserve the concept substantially intact.
- **EVOLVE** — preserve the responsibility, replace the shape/API.
- **REPLACE** — historical implementation is obsolete; retain only lessons.
- **BURY** — not part of Geministrator's future architecture.

---

## Snapshot A — 2025-08-03 pre-IDE orchestrator (`63f4261`)

### Core orchestration

| Historical item | Verdict | New target | Notes |
|---|---|---|---|
| `core/Orchestrator.kt` | **EVOLVE** | `core/orchestration/WorkflowEngine` | Preserve decomposition, bounded parallelism, retries, resumability, integration, escalation. Replace imperative branch/file execution with provider-backed task runs and a DAG scheduler. |
| `MasterPlan` | **EVOLVE** | `WorkflowDefinition` | Replace flat `sub_tasks` with a dependency graph. |
| `SubTask` | **EVOLVE** | `TaskDefinition` | Add dependencies, assigned role, acceptance criteria, artifacts, retry/escalation policy, provider constraints. |
| `WorkflowPlan` | **EVOLVE** | `TaskPlan` / provider plan artifact | A worker's proposed execution plan remains useful, but it must be distinct from the orchestration graph. |
| `WorkflowStep` | **REPLACE** | `AgentTaskRequest` + provider-native actions | Geministrator should not micromanage shell/file commands when Jules or another agent owns execution. |
| `SessionState` | **EVOLVE** | `WorkflowRun` persistence | Preserve resumability. Persist state in app storage/database, not `.orchestrator/session.json` inside the target repo. |
| `executeMasterPlan()` | **EVOLVE** | DAG scheduler | Keep bounded parallel execution and final integration semantics. Add ready/blocked states and dependency resolution. |
| `handleTask()` | **EVOLVE** | task-run state machine | Preserve validation, retries, context carry-forward and escalation. Remove recursive control flow. |
| self-correction loop | **KEEP** | `RetryPolicy` | Failed verification and rejected plans should produce structured retry context. |
| integration branch phase | **EVOLVE** | `IntegrationPolicy` | Preserve isolation and explicit integration. Map primarily to Jules sessions/PRs rather than local branch manipulation. |
| concurrency semaphore | **KEEP** | workflow scheduler concurrency limit | Simple, useful and still correct. Make limits provider-aware later. |
| human decision requests | **KEEP** | `ApprovalGate` / `HumanDecision` | Promote to first-class persisted events and UI inbox items. |
| cleanup-on-failure | **EVOLVE** | provider/resource cleanup policy | Preserve deterministic cleanup, but never delete evidence or logs. |

### Manager

| Historical item | Verdict | New target | Notes |
|---|---|---|---|
| `core/Manager.kt` | **KEEP** | `TaskSupervisor` | Preserve the boundary: manager/supervisor executes an already-approved task plan; it does not invent project scope. |
| `WorkflowStatus` | **EVOLVE** | `TaskRunStatus` sealed model | Expand beyond success/failure/tests-failed to queued, blocked, planning, awaiting approval, running, verifying, retrying, cancelled, escalated. |
| ordered step execution | **EVOLVE** | provider event consumption | Keep deterministic supervision, but execution belongs to the selected agent provider. |
| fail-fast propagation | **KEEP** | workflow policy | A failed prerequisite must block dependents unless policy explicitly permits degradation. |

### Council / roles

| Historical item | Verdict | New target | Notes |
|---|---|---|---|
| `core/council/Architect.kt` | **KEEP** | built-in `AgentRole.ARCHITECT` | Keep architecture/context analysis and architectural review as separate duties. |
| `core/council/Researcher.kt` | **KEEP** | built-in `AgentRole.RESEARCHER` | Keep external research as a distinct pre-implementation responsibility. |
| `core/council/Antagonist.kt` | **KEEP** | built-in `AgentRole.ADVERSARIAL_REVIEWER` | One of the strongest original ideas. Retain as an explicit plan gate with authority to reject and return structured objections. |
| `core/council/TechSupport.kt` | **KEEP** | built-in `AgentRole.RECOVERY` | Generalize from merge-conflict analysis to failed-run diagnosis and recovery planning. |
| `core/council/Designer.kt` | **EVOLVE** | split into Product/UX/Documentation roles | It mixed specification, changelog and historical-memory duties. Split those responsibilities. |
| role-specific prompts | **KEEP** | editable `RoleDefinition.instructions` | Roles should remain sharply scoped and configurable. |
| hard-coded role classes | **REPLACE** | data-driven role definitions + role handlers | Roles are configuration/domain objects; provider execution should be generic. |

### Commands / execution abstraction

| Historical item | Verdict | New target | Notes |
|---|---|---|---|
| `common/AbstractCommand.kt` | **REPLACE** | provider-neutral task/artifact/event contracts | Valuable evidence of attempted separation, but too low-level and locally executable. |
| `common/ExecutionAdapter.kt` | **EVOLVE** | `AgentProvider` | Keep the adapter boundary; redefine it around remote agent runs rather than shell/filesystem primitives. |
| `common/ExecutionResult` | **EVOLVE** | `AgentEvent` / `AgentRunResult` | Structured results stay. Expand to event streams and artifacts. |
| direct `WriteFile` / `RunShell` / `RunTests` commands | **BURY** | provider implementation detail | Core orchestration must not assume these capabilities. |
| direct local Git commands | **BURY** | GitHub/Jules integration layer | Git operations belong to providers/integrations, not the workflow domain. |

### Persistence / history

| Historical item | Verdict | New target | Notes |
|---|---|---|---|
| `.orchestrator/session.json` | **EVOLVE** | local persistent `WorkflowRun` store | Keep resumability, move state out of the managed repository. |
| `.orchestrator/journal.log` | **KEEP** | append-only `WorkflowEvent` log | Promote this to a central architectural primitive and UI data source. |
| `START_TASK` / `TASK_SUCCESS` / `TASK_FAILURE` | **EVOLVE** | typed event taxonomy | Expand into lifecycle, approval, artifact, verification and integration events. |
| `recordHistoricalLesson()` | **EVOLVE** | run postmortems / project memory artifacts | Useful concept; remove it from the Designer role. |

### Provider/config implementation

| Historical item | Verdict | New target | Notes |
|---|---|---|---|
| `common/GeminiService.kt` | **REPLACE** | `providers/jules/JulesProvider` initially | Hard-coded model strategy is obsolete. |
| strategic/flash model split | **EVOLVE** | provider/model capability policy | The intent—different workers for different work—survives, but selection belongs to provider policy. |
| `core/config/ConfigStorage.kt` | **EVOLVE** | multiplatform settings repository | Preserve typed persistent settings. |
| `adapter/CliConfigStorage.kt` | **BURY** | platform settings implementation | CLI-specific storage is archaeological. |
| `adapter/CliAdapter.kt` | **BURY** | none in core | Local execution is not the product. Keep snapshot only for reference. |
| `Main.kt` CLI shell | **BURY** | none | Geministrator is the Compose control room. |
| tokenizer | **BURY** | none unless a later provider requires budgeting | Premature machinery from a different API era. |

---

## Snapshot B — 2025-10-10 workflow resurrection (`9052b8d`)

This commit is valuable mainly because the original orchestration idea resurfaced inside the IDE-era app.

| Historical item | Verdict | New target | Notes |
|---|---|---|---|
| `data/model/geministrator/Plan.kt` | **EVOLVE** | `WorkflowDefinition` / `TaskPlan` | Keep as evidence that plan became a UI/domain object. Rebuild against the DAG model. |
| `data/model/geministrator/DelegatedTask.kt` | **EVOLVE** | `TaskDefinition` + `TaskRun` | Delegation is foundational; separate immutable task definition from execution state. |
| `data/model/geministrator/Geministrator.kt` | **REPLACE** | explicit workflow/run aggregate | Avoid a vague god-object named after the product. |
| `ui/geministrator/GeministratorScreen.kt` | **EVOLVE** | `features/workflows/WorkflowScreen` | Salvage information hierarchy and interaction ideas, not implementation. |
| `ui/geministrator/GeministratorViewModel.kt` | **EVOLVE** | workflow feature ViewModel | Replace placeholder orchestration with domain use-cases/state flows. |
| role enable/disable settings | **KEEP** | `RoleDefinition.enabled` | Built-in roles must be individually configurable. |
| `prompts.json` role configuration concept | **EVOLVE** | persisted/importable `RoleDefinition` templates | Keep data-driven roles; move beyond one static bundled JSON file. |
| SettingsRepository role persistence | **EVOLVE** | multiplatform role/settings repositories | Preserve persistent customization. |
| duplicate/redeclared ViewModel files | **BURY** | none | Period-authentic rubble. |

---

## Cross-cutting concepts

### KEEP

1. **Distinct company roles** with narrow responsibilities.
2. **Orchestrator → supervisor/manager → worker** authority separation.
3. **Parallel isolated work** with an explicit concurrency ceiling.
4. **Adversarial review before execution.**
5. **Verification and retry with failure context.**
6. **Explicit integration phase** after worker completion.
7. **Human escalation only at decision gates.**
8. **Persistent resumable runs.**
9. **Append-only workflow history/event log.**
10. **Configurable/disableable roles.**

### EVOLVE

1. Flat master plan → **DAG workflow**.
2. Branch-per-manager → **provider-owned isolated AgentRun / PR**.
3. Session JSON → **database-backed WorkflowRun**.
4. Logger strings → **typed WorkflowEvent stream**.
5. Role classes → **data-driven AgentRole definitions**.
6. execution adapter → **AgentProvider abstraction**.
7. test failure retry → **general verification/retry/escalation policies**.
8. pre-commit review → **general ApprovalGate model**.

### REPLACE

1. Gemini-specific service layer.
2. JSON command plans containing raw shell/file operations.
3. Recursive orchestration control flow.
4. Core assumptions about local repositories/filesystems.
5. Hard-coded model names and strategic/flash routing.

### BURY

1. Mobile IDE/editor/file-explorer/terminal code.
2. Android Studio and VS Code plugin machinery.
3. CLI as a primary product surface.
4. Direct shell execution from orchestration core.
5. Tokenizer and obsolete API-era plumbing unless a future requirement proves otherwise.

---

## Proposed Geministrator 2 mapping

```text
Historical                         Geministrator 2
──────────────────────────────────────────────────────────────────
Orchestrator                       WorkflowEngine
MasterPlan                         WorkflowDefinition (DAG)
SubTask                            TaskDefinition
Manager                            TaskSupervisor
WorkflowStatus                     TaskRunStatus
SessionState                       WorkflowRun
journal.log                        WorkflowEventStore
Council class                      AgentRole / RoleDefinition
Antagonist                         Adversarial Review Gate
ExecutionAdapter                   AgentProvider
GeminiService                      JulesProvider (first provider)
feature branch                     Provider isolation / PR artifact
RequestUserDecision                ApprovalGate / HumanDecision
TestsFailed retry                  VerificationPolicy + RetryPolicy
integration branch                 IntegrationPolicy
```

---

## Initial built-in role set

These are roles, not providers or models.

1. **Orchestrator** — owns workflow decomposition, dependencies and authority.
2. **Product Manager** — requirements, acceptance criteria and scope.
3. **Researcher** — external/domain research.
4. **Architect** — architecture and repository-impact analysis.
5. **UX/Designer** — interaction/design requirements where relevant.
6. **Implementation Engineer** — code implementation; specialization may be configured per workflow.
7. **QA Engineer** — independent verification against acceptance criteria.
8. **Adversarial Reviewer** — attacks plans/changes for omissions, risk and false confidence.
9. **Code Reviewer** — reviews concrete changes independently of implementation.
10. **Recovery Engineer** — diagnoses failures/conflicts and proposes corrective work.
11. **Release Engineer** — integration/release gates and delivery artifacts.

---

## Event model seed

```text
WORKFLOW_CREATED
WORKFLOW_PLANNING
WORKFLOW_PAUSED
WORKFLOW_RESUMED
TASK_CREATED
TASK_BLOCKED
TASK_READY
AGENT_ASSIGNED
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

---

## Rules derived from the rubble

1. A worker may not approve its own plan or verify its own completion when an independent role is available.
2. Provider-specific concepts do not enter the workflow domain model.
3. Tasks declare dependencies explicitly; parallelism is an outcome of the graph, not a planning adjective.
4. Every state transition emits a typed event.
5. A failed prerequisite blocks dependents until retried, replaced, waived or escalated.
6. Human attention is an explicit gate, never an accidental consequence of a chat window.
7. Historical salvage remains immutable under `salvage/`; modernization happens elsewhere.
8. The editor, terminal and file explorer belong to IDEaz. They do not grow back here.
