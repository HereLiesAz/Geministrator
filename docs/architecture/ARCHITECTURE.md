# Geministrator Architecture

## Product definition

Geministrator is a Compose Multiplatform control room for governed software-development workflows.

> Geministrator is not an agent. It is the company that hires agents.

The product is not an IDE. It does not own source editing, terminals, or generic filesystem tooling. Its job is to turn an objective into an observable, resumable, governed workflow and supervise the people, agents, and systems that execute it.

## Product targets

- Android
- Desktop JVM
- Web JavaScript
- WebAssembly

Web is a first-class runtime target.

## Active modules

```text
shared/       domain, orchestration, persistence, policies, shared Compose UI
providers/    provider adapters, beginning with Jules
androidApp/   Android application
desktopApp/   Desktop application
webApp/       Browser application
```

The repository root Gradle settings define the complete active build graph. Source that is not in that graph is not part of the product.

## Architectural rules

1. Roles are responsibilities, not providers.
2. Providers do not leak into workflow-domain semantics.
3. Tasks declare dependencies explicitly; workflow structure is a DAG.
4. Parallelism is derived from the DAG and policy, not prose.
5. The same worker should not determine the job, execute it, and certify its own success when independent verification is available.
6. Meaningful workflow state is durable and resumable.
7. Human attention appears through explicit decisions and gates.
8. Provider failure must not corrupt workflow state.
9. Shared code must remain portable across Android, Desktop, JS, and Wasm.
10. The workflow UI is a projection of runtime truth, not a second source of execution state.

## Company model

The built-in company includes responsibilities such as:

- Orchestrator
- Product Manager
- Researcher
- Architect
- EPA Representative
- UX Designer
- Implementation Engineer
- Crash Test Dummy
- QA Engineer
- Adversarial Reviewer
- Code Reviewer
- Recovery Engineer
- Release Engineer

A role owns responsibility and authority. A provider supplies execution capability. The same role can be staffed by different providers without changing workflow semantics.

## Workflow runtime

`WorkflowDefinition` describes the dependency graph. `WorkflowRun` and `TaskRun` carry durable execution state.

The engine owns:

- DAG validation
- readiness and blocking
- role/provider assignment
- bounded parallel dispatch
- plan approval
- progress reconciliation
- artifact collection
- verification
- retry and escalation
- persistence and resume

Task progress is executor-neutral. When an executor supplies an exact fraction, Geministrator preserves it. When only qualitative lifecycle evidence exists, the UI may show lifecycle progress without pretending that it is an exact percentage.

## Provider boundary

Provider adapters translate between Geministrator's neutral contracts and external systems. Jules is the first provider.

Provider-specific identifiers, payloads, credentials, and activity schemas stay behind the provider boundary.

## Execution beyond agents

Workflow nodes are not inherently visual representations of agents. Builds, test runners, GitHub Actions, deployments, approvals, and other systems can also be workflow work.

The runtime progress and mindmap projection APIs are already executor-neutral. The remaining engine refactor is to separate task responsibility from the concrete executor so non-agent execution does not need to masquerade as a company role.

## Workflow UI

The main execution surface is the animated H2G2 workflow mindmap.

Each node derives presentation from real workflow state:

- dependency position
- role identity
- status
- provider assignment
- attempt
- progress
- blocking reason
- artifacts

Role personality motion remains local to the role. Nodes also inherit diminishing motion from their workflow ancestry so a branch behaves like a related physical system rather than disconnected animated objects.

Active nodes may express work through shape/motion and by filling the existing node with progress rather than attaching a separate progress surface.

## Artifacts and evidence

Agents and systems communicate through explicit artifacts and events rather than implicit shared transcript state. Verification and integration decisions should rely on concrete evidence wherever possible.

## Delivery

`.github/workflows/multiplatform.yml` is the canonical CI/CD workflow.

Pushes to `main`:

- test shared workflow code
- test and compile provider targets
- build Android
- build a Desktop distribution
- build JS and Wasm web targets
- upload Android and Desktop artifacts
- deploy the JS production bundle to GitHub Pages after a successful build

Android signing is used when repository keystore material is available. Google Play publishing is a separate delivery concern and will use the existing Play service-account secret when enabled.
