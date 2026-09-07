# Milestone 1 — Compose Multiplatform Foundation

Status: complete

This milestone establishes the new Geministrator application shell described in `GEMINISTRATOR_2.md`.

## Build graph

Active Gradle modules:

- `:shared` — shared Compose UI and future shared domain/application code
- `:androidApp` — Android launcher
- `:desktopApp` — JVM desktop launcher
- `:webApp` — browser launcher with Kotlin/JS and Kotlin/Wasm targets

Legacy IDE-era modules remain in repository history/source for reference but are excluded from the active Gradle graph.

## Platform targets

The shared shell now compiles for:

- Android
- Desktop JVM
- Web JavaScript
- WebAssembly

The web application is a first-class target. The browser launcher uses `ComposeViewport`, and the Web module produces both JS and Wasm bundles.

## Toolchain

- Kotlin 2.4.10
- Compose Multiplatform 1.12.0
- Android Gradle Plugin 9.1.1
- Gradle 9.3.1
- compileSdk 37
- targetSdk 36
- JDK 17

## CI verification

`.github/workflows/multiplatform.yml` builds all four targets on every push to `architecture/geministrator-2` and on pull requests to `main`.

Verified commands:

```text
:androidApp:assembleDebug
:desktopApp:compileKotlin
:webApp:jsBrowserProductionWebpack
:webApp:wasmJsBrowserProductionWebpack
```

All four passed in GitHub Actions run `34149830138`.

## Shared shell

`shared/src/commonMain/kotlin/com/hereliesaz/geministrator/App.kt` contains the first responsive shared UI shell. It establishes the future top-level product surfaces:

- Projects
- Workflows
- Company
- Runs
- Inbox
- Settings

No editor, terminal, preview, file explorer, local Git engine, ADK runtime, Hilt graph, Room database, or IDE-specific code participates in the new application foundation.

## Next milestone

Milestone 2 builds the provider-neutral domain core:

1. identifiers and core value types
2. `Project`
3. `WorkflowDefinition`
4. `TaskDefinition`
5. DAG validation
6. `WorkflowRun`
7. `TaskRun`
8. typed `WorkflowEvent`
9. `RoleDefinition`
10. `AgentProvider` contracts

The Jules implementation comes only after those provider-neutral contracts are stable.
