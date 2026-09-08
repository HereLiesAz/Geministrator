# Geministrator Persistence

## Purpose

Workflow execution must survive Android process death, desktop restarts, browser reloads, and provider sessions that outlive the UI process.

The workflow engine depends only on repository interfaces in `shared`. Provider sessions remain identified by `assignedProviderId` and `providerRunId`, so a resumed run reconnects to the existing provider session rather than creating a duplicate.

## Current durable backend

`SettingsWorkflowPersistence` is the first concrete cross-platform backend. It stores a versioned serialized workflow snapshot through Multiplatform Settings.

Platform backing stores are supplied by the library:

- Android: SharedPreferences
- Desktop/JVM: Java Preferences
- Web JS: browser localStorage
- Web Wasm: browser localStorage

The backend persists:

- projects
- prepared workflow definitions
- workflow runs
- append-only workflow events
- role definitions
- artifacts
- approval gates

The snapshot is written under a single storage key while guarded by a mutex. This gives the shared engine a restart-safe baseline without making workflow code depend on Android, JVM, or browser APIs.

## Schema versioning

Every snapshot includes a schema version. The current schema is `1`.

Readers reject snapshots created by a newer unsupported schema. Future schema changes must add an explicit migration path rather than silently interpreting incompatible data.

## Secrets

Provider credentials, API keys, OAuth tokens, private signing material, and secret values MUST NOT be stored in `WorkflowPersistence`.

Workflow data may contain the names of secrets required by an `EnvironmentSpecification`, but never their values. Provider credentials require a separate platform-secure credential store or a server-side/BFF credential boundary for Web.

## Scale boundary

The Settings-backed snapshot is the restart-safe baseline, not the final high-volume event database.

The repository interfaces intentionally permit later replacement with indexed storage without changing the workflow engine. Expected evolution:

- Android/Desktop: transactional SQL-backed repositories when event/run volume requires it.
- Web: IndexedDB-backed repositories for larger datasets and indexed queries.

That migration must preserve the same repository contracts and persisted IDs.

## Resume invariant

A persisted active task with both `assignedProviderId` and `providerRunId` MUST reconnect to that existing provider run. Resume MUST NOT create a replacement provider session merely because the Geministrator process restarted.
