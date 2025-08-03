pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/")
        maven("https://jitpack.io")
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.23"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
        id("org.jetbrains.intellij") version "1.17.3"
        application
        id("org.jetbrains.compose") version "1.6.10"
        id("application")
    }
}

rootProject.name = "Geministrator"

include(
    ":common",
    ":core",
    ":cli",
    ":plugins:android-studio-plugin"
)
