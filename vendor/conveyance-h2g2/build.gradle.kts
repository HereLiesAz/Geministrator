import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    `maven-publish`
}

group = "com.hereliesaz.conveyance"
version = "0.1.0"

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    androidLibrary {
        namespace = "com.hereliesaz.conveyance.h2g2"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }
    jvm("desktop")
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // Pin the exact Conveyance revision this binding was built against. Floating
            // main-SNAPSHOT dependencies make KMP metadata non-reproducible and can resolve stale
            // target publications through JitPack.
            api("com.github.HereLiesAz.Conveyance:conveyance-core:468371de06a903b1a6bdcf812197eef4a81afd3e")
            api("com.github.HereLiesAz.Conveyance:conveyance-compose:468371de06a903b1a6bdcf812197eef4a81afd3e")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Kotlin Multiplatform registers one publication per target on its own; there is nothing to
// create here, only a shared description for whichever one a consumer ends up resolving.
publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Conveyance h2g2")
            description.set(
                "The h2g2 style system -- hues, ground-rotation surfaces, workflow routes, and the 8-step Jost type scale -- ported from HG2Gui.",
            )
        }
    }
}
