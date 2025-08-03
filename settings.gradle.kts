pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.23"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
        id("org.jetbrains.intellij") version "1.17.3"
        id("application")
    }
}

rootProject.name = "GeminiOrchestrator"

include(
    ":common",
    ":core",
    ":adapters:adapter-cli",
    ":adapters:adapter-android-studio",
    ":products:cli",
    ":products:android-studio-plugin"
)