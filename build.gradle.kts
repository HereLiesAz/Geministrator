plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

allprojects {
    group = "com.hereliesaz.GeminiOrchestrator"
    version = "1.0.0"

    repositories {
        mavenCentral()
        google()
        maven(url = "https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    kotlin {
        jvmToolchain(17) // This tells Kotlin and Java tasks to use JDK 17 for compilation
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
    // 1. Apply plugins using the correct syntax for this context
    apply(plugin = "org.jetbrains.compose")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    // Fix for Line 38:
    // Inside configure<org.jetbrains.compose.ComposeExtension>, 'this' is already the extension.
    configure<org.jetbrains.compose.ComposeExtension> {
        // 'desktop' is a property of the ComposeExtension, not 'compose.desktop'
        desktop {
            // You can add desktop-specific configurations here later,
            // e.g., mainClass = "com.hereliesaz.geminiorchestrator.cli.MainKt"
            // For now, an empty block is sufficient to resolve the 'compose' reference.
        }
    }
    dependencies {
        implementation(project(":core"))
        implementation(project(":adapters:adapter-cli"))
        // Fix for Line 49:
        // Explicitly get the ComposeExtension using 'the<Type>()'
        // and then access its properties like 'desktop.currentOs'
        implementation(compose.desktop.currentOs) // This is the correct notation
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
        // This module has no external dependencies, only internal ones.
    }
}
