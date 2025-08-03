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
        id("org.jetbrains.intellij") version "2.2.0"
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