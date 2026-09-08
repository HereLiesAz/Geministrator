# Geministrator

Geministrator is a Compose Multiplatform control room for governed software-development workflows across interchangeable executors and agentic providers.

> Geministrator is not an agent. It is the company that hires agents.

## Targets

- Android
- Desktop
- Web (JavaScript and WebAssembly)

## Architecture

The active product lives in:

- `shared/` — domain, workflow engine, shared UI, persistence, policies, and runtime projection
- `providers/` — provider integrations, beginning with Jules
- `androidApp/` — Android launcher
- `desktopApp/` — desktop launcher
- `webApp/` — browser launcher

See `docs/architecture/ARCHITECTURE.md` for the current architecture.

## Build

```bash
./gradlew :shared:desktopTest
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:packageDistributionForCurrentOS
./gradlew :webApp:jsBrowserProductionWebpack
./gradlew :webApp:wasmJsBrowserProductionWebpack
```

Pushes to `main` build Android, Desktop, JavaScript, and WebAssembly targets. The JavaScript production bundle is deployed to GitHub Pages after a successful build.

## License

This project is licensed under the MIT License. See `LICENSE` for details.
