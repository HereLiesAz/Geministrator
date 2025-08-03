// Project-level build file: Defines plugins and versions for all modules.


    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
    application
    }
