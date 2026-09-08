# The Haive architecture

## Product definition

The Haive is a Compose Multiplatform control room for governed software-development workflows.

> The Haive is not an agent. It is the company that hires agents.

Its job is to turn an objective into explicit work, establish dependency and verification structure, assign that work to appropriate executors, observe the run, and surface only the human decisions that genuinely require a person.

The Haive is not an IDE. Source editing, terminals, and generic filesystem tooling are outside the product boundary.

## First principles

1. A task describes **what must happen**.
2. A role describes **responsibility and authority**.
3. An executor describes **who or what performs the work**.
4. A provider is one possible execution mechanism, not workflow semantics.
5. Dependencies are explicit edges in a DAG.
6. Parallelism is derived from the graph and policy.
7. The same worker should not define, execute, and certify its own work when independent verification is available.
8. Human intervention happens through explicit gates and decisions.
9. Provider or executor failure must not corrupt durable workflow state.
10. The UI projects runtime truth; it does not invent a second source of execution state.

## Targets

- Android — application ID and namespace `com.hereliesaz.haive`
- Desktop JVM
- Web JavaScript
- WebAssembly

Shared code must remain portable across all four targets.

## Active modules

```text
shared/       domain, workflow engine, policies, persistence, shared Compose UI
providers/    provider adapters, beginning with Jules
androidApp/   Android launcher
desktopApp/   Desktop launcher
webApp/       browser launcher
```

The root Gradle settings are the authoritative active build graph.

## Work, roles, and executors

A workflow node is a unit of work, not a synonym for an agent.

Examples of valid execution include:

- a Jules-backed Implementation Engineer
- a human approval gate
- a GitHub Actions build
- a test runner
- a deployment job
- a release operation
- a future provider-backed role

The current domain still carries role assignment in places where the earlier engine assumed agent-backed work. The direction is executor-neutral: responsibility and execution mechanism are separate concepts, and non-agent systems must not masquerade as fake employees merely to satisfy a type.

## Company model

Built-in responsibilities include:

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

Roles are data. They carry responsibility, instructions, capabilities, and authority. A role can be staffed by different providers without changing its meaning.

## Workflow definition and run state

`WorkflowDefinition` is immutable execution structure: tasks, dependencies, acceptance requirements, policies, and gates.

`WorkflowRun` and `TaskRun` are durable state. They carry status, attempts, assignments, provider/executor identifiers, artifacts, blocking reasons, and progress.

The engine owns:

- DAG validation
- readiness and blocking
- bounded parallel dispatch
- role/executor selection
- plan approval
- progress reconciliation
- artifact collection
- independent verification
- retries and escalation
- durable state transitions
- resume after process death or restart

## Progress

Progress belongs to `TaskRun`, not to agents.

An executor may provide an exact fraction. When it does, the runtime preserves that value. Some executors, including the currently exposed Jules activity model, provide only qualitative progress. In that case The Haive may present lifecycle progress such as planning, running, and verifying without pretending it is an exact percentage.

This makes the same UI capable of representing both a provider activity like “Writing tests” and an automated executor that knows “7 of 11 steps complete.”

## Provider boundary

Provider adapters translate external APIs into neutral runtime contracts.

Jules is the first provider. Jules source IDs, session IDs, request payloads, credentials, and activity schemas remain inside the Jules adapter.

Future providers can implement the same neutral contracts without changing workflow semantics.

## Artifacts and evidence

Workers and systems communicate through explicit artifacts and events rather than implicit shared transcript inheritance.

Examples include requirements, research, architecture, plans, code changes, test results, reviews, verification evidence, failure analysis, pull requests, and release outputs.

Verification and integration decisions should rely on concrete evidence rather than a worker merely claiming completion.

## The workflow mindmap

The primary execution surface is the animated H2G2 workflow mindmap.

Each visible node is projected from real workflow state, including:

- graph position and dependencies
- task identity
- responsibility/role identity when present
- status
- executor/provider assignment
- retry attempt
- blocking reason
- artifacts
- progress and progress message

Role personality motion remains local to the node. A node also inherits diminishing motion from its workflow ancestry, so branches behave like related physical systems rather than disconnected animated widgets.

Active nodes can express work through motion and by filling the existing node with progress instead of attaching a conventional progress bar.

## Human attention

The Inbox is for unresolved decisions and gates, not general conversation. Human attention is treated as scarce and should be requested only when policy or judgment actually requires it.

## Security boundary

Workflow persistence must never contain provider credentials, OAuth tokens, private signing material, or secret values. The runtime may persist references to required secret names, but secret values belong in platform-secure credential handling or an external service boundary.

See the [privacy policy](../PRIVACY.md) for user-facing data handling.

## Delivery

`.github/workflows/multiplatform.yml` is the canonical CI/CD workflow.

Pushes to `main`:

- run shared workflow tests
- test and compile provider targets
- build Android release artifacts
- build a Desktop distribution
- build JS and Wasm web targets
- upload Android and Desktop artifacts
- deploy the JS production bundle to GitHub Pages after a successful build

Android signing is used when signing secrets are available. Play publishing is a separate delivery step and will use the repository Play service-account secret when enabled.
