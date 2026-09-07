pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localH2g2 = file("vendor/conveyance-h2g2")
if (localH2g2.exists()) {
    includeBuild(localH2g2) {
        dependencySubstitution {
            substitute(module("com.github.HereLiesAz:conveyance-h2g2"))
                .using(project(":"))
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Geministrator"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":shared")
include(":providers:jules")
include(":androidApp")
include(":desktopApp")
include(":webApp")
