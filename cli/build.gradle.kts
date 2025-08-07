import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask
// Module-level build file for the 'app' module.
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    application
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Testing Dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    implementation(kotlin("test"))

    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

tasks.test {
    useJUnitPlatform()
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