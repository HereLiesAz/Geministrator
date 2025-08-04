import org.jetbrains.intellij.tasks.PatchPluginXmlTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    mavenCentral()
}
intellij { // Add this block
    pluginXml { // This is the correct sub-block for patchPluginXml properties
        changeNotes.set("""
            ### 0.1.0
            - Initial release of the Geministrator plugin.
        """.trimIndent())
    }
}
// For a detailed guide, see:
// https://github.com/JetBrains/gradle-intellij-plugin/


dependencies {
    // Depend on the CLI module to get access to the core logic
    implementation(project(":cli"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}


