plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij")
    id("org.jetbrains.kotlin.plugin.serialization")
}

sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}


intellij {
    version.set("2023.3.6")
    type.set("IC") // Or "IU" for Ultimate
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes.set("Initial stable release.")
}