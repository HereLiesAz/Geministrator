plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.hereliesaz.geministrator.MainKt"

        nativeDistributions {
            packageName = "Geministrator"
            packageVersion = "2.0.0"
        }
    }
}
