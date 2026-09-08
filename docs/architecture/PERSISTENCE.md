# Persistence

## Goal

A workflow should survive Android process death, a desktop restart, a browser reload, or a provider session that outlives the UI process.

The engine therefore treats durable workflow state as a first-class product requirement rather than as UI state.

## Current backend

`SettingsWorkflowPersistence` is the current cross-platform persistence implementation. It stores a versioned serialized workflow snapshot through Multiplatform Settings.

Current platform backing stores are:

- Android: SharedPreferences
- Desktop/JVM: Java Preferences
- Web JS: browser localStorage
- Web Wasm: browser localStorage

The persisted model includes:

- projects
- prepared workflow definitions
- workflow runs
- task runs
- append-only workflow events
- role definitions
- artifacts
- approval gates

Writes are guarded so the engine has a coherent restart-safe baseline.

## What is deliberately not persisted here

`WorkflowPersistence` must not contain:

- provider API keys
- OAuth access or refresh tokens
- passwords
- private signing keys
- keystore contents
- service-account credentials
- arbitrary secret values

A workflow may carry the **name** of a secret required by an environment, but not the secret itself.

Credentials belong in platform-secure storage or, where a browser cannot safely hold a credential, behind an appropriate service boundary.

## Resume invariant

A task with an existing executor/provider run identifier must reconnect to that existing run when possible. Restarting The Haive must not create duplicate external work merely because the local process restarted.

## Schema versioning

Snapshots include a schema version. Readers reject newer unsupported schemas. Any incompatible schema change requires an explicit migration path rather than silent reinterpretation.

## Scale boundary

The Settings-backed snapshot is a restart-safe baseline, not the final high-volume event database.

The repository contracts are intentionally replaceable. Likely future storage includes transactional SQL on Android/Desktop and IndexedDB on Web when event volume or indexed queries justify it.

A storage migration must preserve domain IDs and resume behavior.

## User control

Local workflow data remains on the device/browser until the application removes it or the user clears application/browser storage. Data created at external providers is governed separately by those services.

See [`../PRIVACY.md`](../PRIVACY.md).
