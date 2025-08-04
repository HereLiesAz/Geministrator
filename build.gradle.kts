// The root-level plugins block was conflicting with plugin management in settings.gradle.kts.
// It has been removed to centralize plugin versioning.

plugins {
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false
    id("application")
    id("org.panteleyev.jpackageplugin") version "1.7.3"
    id("com.android.application") version "8.12.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
    id("org.jetbrains.intellij.platform") version "2.2.0" apply false
}
