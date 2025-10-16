allprojects {
    configurations.all {
        resolutionStrategy {
            // The Google ADK library has a dependency on an older version of protobuf-java.
            // A newer version is being pulled in transitively from another dependency,
            // causing a conflict. This forces the version to the one required by ADK.
            force("com.google.protobuf:protobuf-java:3.25.8")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
