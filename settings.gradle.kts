pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.23"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
        id("application")
    }
}

rootProject.name = "Geministrator"

include(":cli")