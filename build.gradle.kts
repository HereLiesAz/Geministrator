import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.jetbrains.intellij") apply false
}

allprojects {
    group = "com.hereliesaz.GeminiOrchestrator"
    version = "1.0.0"

    repositories {
        mavenCentral()
        google()
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
        implementation(project(":common"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    }
}

project(":products:cli") {
    apply(plugin = "application")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    application {
        mainClass.set("com.hereliesaz.geminiorchestrator.cli.MainKt")
    }
    dependencies {
        implementation(project(":core"))
        implementation(project(":adapters:adapter-cli"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    }
}

project(":adapters:adapter-cli") {
    dependencies {
        implementation(project(":core"))
    }
}

project(":adapters:adapter-android-studio") {
    dependencies {
        implementation(project(":core"))
    }
}

project(":common") {
    dependencies {
        // No external dependencies needed for this module.
    }
}