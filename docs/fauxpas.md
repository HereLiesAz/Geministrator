# Faux Pas (Common Pitfalls)

This document lists common issues, pitfalls, and their resolutions that have been encountered during the development of the Jules IDE project.

## Persistent Build Errors

A significant challenge during the initial development was a persistent and difficult-to-diagnose build error related to Gradle dependency conflicts.

### Symptoms

-   The build would fail with a `Too many files created` error, even when running a `clean` task. This indicates a fundamental issue with the build environment's interaction with Gradle.
-   When the "too many files" error was bypassed, the build would fail with `Unresolved reference` errors for Gradle plugin aliases, or with `AAR metadata check` failures, indicating version incompatibilities between the Android Gradle Plugin (AGP) and various Jetpack Compose libraries.

### Root Cause

The root cause was a complex and fragile web of dependencies between:

1.  The Android Gradle Plugin (AGP) version.
2.  The Gradle wrapper version.
3.  The versions of the various Jetpack Compose libraries.
4.  The build requirements of external libraries (such as Chaquopy).

### Resolution Strategy

The most effective strategy for resolving these issues is to:

1.  **Isolate Modules**: When adding new modules, build them in isolation first (`./gradlew :new_module:build`) to ensure their internal dependencies and plugin configurations are correct before integrating them with the main application.
2.  **Align AGP and Gradle Versions**: Ensure that the AGP version specified in `gradle/libs.versions.toml` is compatible with the Gradle version specified in `gradle/wrapper/gradle-wrapper.properties`. The build logs will often provide guidance on the required versions.
3.  **Unify Build Environments**: Whenever possible, ensure that all modules in the project are of the same type (e.g., all Android libraries) to maintain a consistent build environment and dependency resolution mechanism. This was the key to resolving the issues with the `:jules-api-client` module.
4.  **Bypass Version Catalog for Troubleshooting**: If a module is still failing to resolve plugin aliases from the version catalog, temporarily bypass the catalog and declare the plugin versions explicitly in the module's `build.gradle.kts` file to unblock the build. This can help isolate the problem.