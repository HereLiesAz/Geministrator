plugins {
    id("org.jetbrains.kotlin.jvm")
    id("application")
    id("org.jetbrains.kotlin.plugin.serialization")
}

application {
    mainClass.set("MainKt")
}

sourceSets.main {
    java.srcDirs("src")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
}