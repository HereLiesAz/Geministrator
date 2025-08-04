// The root-level plugins block was conflicting with plugin management in settings.gradle.kts.
// It has been removed to centralize plugin versioning.

plugins {
    // The intellij platform plugin is applied here because it's not a standard
    // Android/Kotlin plugin and is best managed at the root level for IDE tooling.
}
