import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.plugins.JavaPluginExtension

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.jetbrains.intellij")apply false
}

allprojects {
    group = "com.hereliesaz.geministrator"
    version = "1.0.0"

    repositories {
        mavenCentral()
        google()
        maven(url = "https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    }
}