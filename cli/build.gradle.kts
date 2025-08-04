import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask
// Module-level build file for the 'app' module.
plugins {
    kotlin("jvm")
    `java-library`
    application
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.panteleyev.jpackageplugin")
}

group = "com.hereliesaz.geministrator"
version = "1.1.0"

// Add sourceSets block to include resources
sourceSets {
    main {
        resources {
            srcDirs("src/main/resources")
        }
    }
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
    mavenLocal()
    maven("https://jitpack.io")
}

application {
    mainClass.set("com.hereliesaz.geministrator.MainKt")
    applicationName = "geministrator"
}

dependencies {
    // All project dependencies are now here
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

// This task gathers all dependency JARs into one place for the installer.
val copyDependencies by tasks.registering(Copy::class) {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("jpackage-input/libs"))
}

// This task copies your application's own JAR into the installer's input directory.
val copyJar by tasks.registering(Copy::class) {
    from(tasks.jar)
    into(layout.buildDirectory.dir("jpackage-input"))
}

// This configures the jpackage task, which creates the native installer.
tasks.named<JPackageTask>("jpackage") {
    dependsOn(copyDependencies, copyJar)
    appName.set("Geministrator")
    appVersion.set(project.version.toString())
    vendor.set("HereLiesAz")
    copyright.set("Copyright (c) 2025 HereLiesAz")
    mainJar.set(tasks.jar.get().archiveFileName.get())
    // Correctly specify the main class for the launcher
    mainClass.set(application.mainClass.get())

    // Uses the 'type' property with the correct 'ImageType' enum.
    type.set(ImageType.APP_IMAGE)
    // The input directory must contain your app's JAR and a 'libs' folder with all dependencies.
    input.set(layout.buildDirectory.dir("jpackage-input"))
    destination.set(layout.buildDirectory.dir("installer"))
}