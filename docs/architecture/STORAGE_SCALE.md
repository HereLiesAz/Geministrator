# Storage Scale Decision

## Current approach

Workflow state is persisted via platform Settings APIs (Android SharedPreferences, Desktop
java.util.prefs, Web localStorage). Each platform's implementation serializes the full
`WorkflowRun` + events journal as JSON strings, keyed by `WorkflowRunId`.

## When to migrate to a structured backend

### Stay on Settings while:
- Total serialized workflow state stays below 1 MB per run.
- The number of active runs per project stays in single digits.
- Event journal length stays below ~500 events per run (empirically around 50 KB serialized).
- Artifact payloads are stored as URIs (not inline content).
- The app does not need cross-device sync, multi-user access, or server-side querying.

### Trigger migration to SQL/IndexedDB when:
- A single run's serialized state exceeds 500 KB (settings writes become noticeably slow).
- The event journal must be streamed rather than loaded in full (memory pressure).
- Artifacts with inline content are needed (binary blobs, large text outputs).
- The app adds a web/server component that needs shared state across sessions or devices.
- Background sync or push notifications require server-readable state.

## Migration path

The `WorkflowPersistence` interface and `InMemoryWorkflowPersistence` / platform
implementations use the same contract. A SQL or IndexedDB backend implements the same
interface. No runtime code changes outside the persistence layer.

For Android: Room (SQLite); for Desktop: SQLite via JDBC; for JS/Wasm: IndexedDB via the
Web Storage API. All three share the same serialization format; only the storage call
sites differ.

## Decision

Do not migrate until one of the trigger conditions above is met and measured on a real run.
Settings-backed storage is reliable, requires no schema migrations beyond the existing
JSON versioning, and adds no dependencies. Premature migration adds complexity without
a measurable user benefit.

When migration is triggered: implement the SQL backend behind `WorkflowPersistence`
and run the existing persistence tests against both implementations before shipping.
