pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://chaquo.com/maven") }
    }
    plugins {
        id("com.chaquo.python")
        id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        maven("https://jitpack.io")
        maven { url = uri("https://chaquo.com/maven") }
    }
}


rootProject.name = "Geministrator"

include(":app")
include(":jules-api-client")
include(":github-api-client")