# Geministrator

Geministrator is a Compose Multiplatform control room for governed software-development workflows across interchangeable agentic providers.

> Geministrator is not an agent. It is the company that hires agents.

## Targets

- Android
- Desktop
- Web (JavaScript and WebAssembly)

## Current status

Geministrator 2 is being rebuilt from the project's original orchestration concepts. The active application foundation lives in:

- `shared/`
- `androidApp/`
- `desktopApp/`
- `webApp/`

The older IDE-era implementation remains in the repository for historical reference but is no longer part of the active Gradle build.

## Architecture

See:

- `docs/architecture/GEMINISTRATOR_2.md`
- `docs/architecture/MILESTONE_1.md`
- `SALVAGE_MANIFEST.md`

Historical snapshots are preserved under `salvage/` and should remain immutable.

## Build

```bash
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlin
./gradlew :webApp:jsBrowserProductionWebpack
./gradlew :webApp:wasmJsBrowserProductionWebpack
```

## License

This project is licensed under the MIT License. See `LICENSE` for details.
