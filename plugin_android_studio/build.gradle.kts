plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.intellij")
}

repositories {
    mavenCentral()
}

// For a detailed guide, see:
// https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2.7.0") // Target IDE version [cite: 863]
    type.set("IC") // Community Edition. Use "IU" for Ultimate. [cite: 863]
    plugins.set(listOf("org.jetbrains.kotlin")) // [cite: 863]
}

dependencies {
    // Depend on the CLI module to get access to the core logic
    implementation(project(":cli"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes.set("""
        ### 0.1.0
        - Initial release of the Geministrator plugin.
    """.trimIndent())
}