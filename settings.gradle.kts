// settings.gradle.kts
rootProject.name = "gemini-orchestrator"

include(
    ":common",
    ":core",
    ":adapters:adapter-cli",
    ":adapters:adapter-android-studio",
    ":products:cli",
    ":products:android-studio-plugin"
)

// build.gradle.kts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.23" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
    id("org.jetbrains.intellij") version "1.17.3" apply false
}

allprojects {
    group = "com.gemini.orchestrator"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

project(":products:android-studio-plugin") {
    apply(plugin = "org.jetbrains.intellij")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    intellij {
        version.set("2023.3.6")
        type.set("IC")
    }

    dependencies {
        implementation(project(":core"))
        implementation(project(":adapters:adapter-android-studio"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    }

    tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
        changeNotes.set("Initial stable release.")
    }
}

project(":core") {
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    }
}

project(":products:cli") {
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    application {
        mainClass.set("com.gemini.orchestrator.cli.MainKt")
    }
    dependencies {
        implementation(project(":core"))
        implementation(project(":adapters:adapter-cli"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    }
}
