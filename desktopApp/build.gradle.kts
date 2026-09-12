import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)
    implementation(projects.providers.jules)
    implementation(compose.desktop.currentOs)
    implementation(libs.ktor.client.cio)
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.hereliesaz.geministrator.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "TheHaive"
            // Strip pre-release suffix — packageVersion must be x.y.z.
            packageVersion = providers.gradleProperty("app.versionName").get().substringBefore("-")
            description = "The Haive — agentic workflow orchestration"
            copyright = "© 2026 HereLiesAz"

            linux {
                iconFile.set(rootProject.file("branding/haive-icon-color.png"))
            }
            macOS {
                val icns = rootProject.file("branding/haive-icon.icns")
                if (icns.exists()) iconFile.set(icns)
            }
            windows {
                val ico = rootProject.file("branding/haive-icon.ico")
                if (ico.exists()) iconFile.set(ico)
                menuGroup = "The Haive"
                upgradeUuid = "e1d4b3c2-5f6a-4b8e-9d7c-0a1b2c3d4e5f"
            }
        }
    }
}
