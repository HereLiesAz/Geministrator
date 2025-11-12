import org.gradle.internal.impldep.com.jcraft.jsch.ConfigRepository.defaultConfig

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.github.HereLiesAz:julesapisdk:1.0.1")
    implementation(project(":github-api-client"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    testImplementation(libs.system.rules)

}

tasks.withType<Test> {
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED")
}
