allprojects {
    configurations.all {
        resolutionStrategy {
            force("jakarta.annotation:jakarta.annotation-api:1.3.5")
            force("org.glassfish.hk2.external:jakarta.inject:2.6.1")
        }
        exclude(group = "commons-logging", module = "commons-logging")
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
