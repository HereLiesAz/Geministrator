plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// CI publishes the pinned checkouts with their native coordinates. Rewriting only Maven
// metadata leaves embedded Kotlin/JS package versions inconsistent with the resolved graph.
if (providers.gradleProperty("haive.useMavenLocalH2g2").orNull == "true") {
    allprojects {
        configurations.configureEach {
            resolutionStrategy.dependencySubstitution {
                substitute(module("com.github.HereLiesAz:conveyance-h2g2:faa80ed5aa063f6bdab42163d7b021123b248254"))
                    .using(module("com.hereliesaz.conveyance:conveyance-h2g2:0.1.0"))
                substitute(module("com.github.HereLiesAz.Conveyance:conveyance-core:468371de06a903b1a6bdcf812197eef4a81afd3e"))
                    .using(module("com.hereliesaz.conveyance:conveyance-core:0.1.0"))
                substitute(module("com.github.HereLiesAz.Conveyance:conveyance-compose:468371de06a903b1a6bdcf812197eef4a81afd3e"))
                    .using(module("com.hereliesaz.conveyance:conveyance-compose:0.1.0"))
            }
        }
    }
}
