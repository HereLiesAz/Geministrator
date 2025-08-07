plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

repositories {
    mavenCentral()
}

// For a detailed guide, see:
// https://github.com/JetBrains/gradle-intellij-plugin/


dependencies {
    // Depend on the CLI module to get access to the core logic for MAIN code
    implementation(project(":cli"))
    // Add the same dependency for TEST code
    testImplementation(project(":cli"))

    // Add a dependency for the test framework
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

// The IntelliJ plugin will configure the test task, but we ensure it uses JUnit Platform.
tasks.withType<Test> {
    useJUnitPlatform()
}