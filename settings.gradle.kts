pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        maven("https://jitpack.io")
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.0.0"
        id("org.jetbrains.kotlin.android") version "2.0.0" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
        id("application")
        id("org.panteleyev.jpackageplugin") version "1.7.3"
        id("com.android.application") version "8.4.1" apply false
        id("org.jetbrains.intellij.platform") version "2.7.0"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        maven("https://jitpack.io")
    }
}


rootProject.name = "geministrator"

include(":cli")
include(":app_android")
include(":plugin_android_studio")
include(":plugin_vscode")