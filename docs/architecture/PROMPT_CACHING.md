# Prompt Caching Strategy

Geministrator must treat prompt caching as a provider optimization, not as workflow semantics.

## Core rule

The workflow engine owns prompt structure. Providers own cache mechanics.

A task request is split into two ordered prompt regions:

1. `stablePrefix` — standing instructions, role instructions, project conventions, approved specifications, architecture, reusable tool guidance, and other context expected to repeat across related agent runs.
2. `dynamicContext` — the current task objective, current artifacts, attempt-specific failure context, recent events, and other volatile information.

Stable context must come first whenever provider behavior permits it. Dynamic context belongs at the end. This maximizes prefix reuse for providers that cache matching prefixes while keeping the domain independent of any provider API.

## Provider-neutral contract

`PromptContext` carries:

- stable prompt blocks
- dynamic prompt blocks
- `PromptReusePolicy`
- an optional cache namespace

`AgentCapabilities.promptCaching` reports the mechanisms a provider can actually support:

- `Unsupported`
- `ImplicitPrefix`
- `ExplicitReusableContext`
- `ExplicitBreakpoints`
- `SessionScoped`

The workflow engine may prefer reuse but must never require a cache hit for correctness.

## Provider mapping

### Jules

The public Jules REST API currently exposes sessions as contiguous units of work and supports follow-up messages within a session. It does not currently document explicit prompt-cache resources or cache-control request fields. `JulesProvider` should therefore advertise `SessionScoped` reuse only when it can preserve context through the same Jules session. It must not fabricate cache-hit telemetry.

For separate Jules sessions, Geministrator should still keep the stable prefix structurally identical so that any provider-side optimization remains possible without depending on undocumented behavior.

### Gemini API

Gemini 2.5 and newer models support implicit context caching, and the `generateContent` API can also use explicit cached-content resources. Stable common content should be placed first. A future direct `GeminiProvider` may map `stablePrefix` to explicit cached content when economical and otherwise rely on implicit caching.

### OpenAI

OpenAI supports prompt caching for eligible models, including stable-prefix reuse and explicit cache controls on current Responses APIs. A future `OpenAIProvider` should preserve deterministic stable-prefix ordering, use an appropriate cache namespace/key, and expose usage telemetry when returned by the API.

### Anthropic

Anthropic supports prompt caching and explicit cache breakpoints on supported APIs. A future `ClaudeProvider` may map stable prompt boundaries to provider cache-control breakpoints while retaining the same provider-neutral `PromptContext` contract.

## What belongs in the stable prefix

Good candidates:

- company-wide operating rules
- role definition and standing instructions
- repository conventions
- architecture constraints
- approved product specification
- approved pre-code verification contract
- tool-use rules
- unchanged environment specification

Poor candidates:

- current retry error
- latest test output
- current task-specific request
- timestamps
- volatile branch or PR state
- recent agent messages

## Pre-code testing interaction

Pre-code verification artifacts are especially valuable cache candidates because they become immutable after approval. The same approved specification, architecture, and verification contract may be consumed by the implementation engineer, Crash Test Dummy implementation pass, QA engineer, code reviewer, and recovery engineer.

The implementation agent must not be allowed to edit approved pre-code verification artifacts. Any requested revision creates a new approval event and therefore a new stable-context version/cache namespace.

## Cache identity

Do not use provider cache IDs as domain identity.

A cache namespace should be derived from stable logical inputs such as:

- project
- workflow definition/version
- role definition/version
- approved specification version
- approved verification-contract version

Provider adapters may translate that namespace into their own cache key/resource/breakpoint mechanism.

## Observability

When a provider reports cache metrics, Geministrator should eventually record:

- reusable input tokens
- cache-hit tokens
- cache-write tokens if applicable
- estimated uncached cost
- estimated cached cost
- latency impact

These are operational metrics only. They must never change task correctness or approval state.
