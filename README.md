# The Haive

**The Haive** is a Compose Multiplatform control room for governed software-development workflows across people, agents, and automated executors.

> The Haive is not an agent. It is the company that hires agents.

The Haive turns an objective into an explicit workflow, staffs or assigns its work, watches the run, enforces gates and verification, and keeps execution observable and resumable.

## What it is

The Haive is an orchestration product, not an IDE. It does not own source editing, terminals, or a generic file explorer. Its primary interface is the live workflow itself: an H2G2-inspired animated mindmap whose nodes represent real work and whose state comes from the runtime.

Workflow nodes may be performed by AI providers such as Jules, by people, or by systems such as GitHub Actions, test runners, and deployment jobs. Progress belongs to the task run, not to a particular kind of worker.

## Targets

- Android — application ID `com.hereliesaz.haive`
- Desktop JVM
- Web — JavaScript and WebAssembly

## Repository map

- `shared/` — domain, orchestration, persistence, policies, runtime projection, shared Compose UI
- `providers/` — provider adapters, beginning with Jules
- `androidApp/` — Android launcher
- `desktopApp/` — Desktop launcher
- `webApp/` — browser launcher
- `docs/` — current product documentation and privacy policy
- `branding/` — locked source brand and icon assets

## Documentation

Start with [`docs/README.md`](docs/README.md).

- [Architecture](docs/architecture/ARCHITECTURE.md)
- [Persistence](docs/architecture/PERSISTENCE.md)
- [Prompt caching](docs/architecture/PROMPT_CACHING.md)
- [Branding](docs/BRANDING.md)
- [Privacy policy](docs/PRIVACY.md)
