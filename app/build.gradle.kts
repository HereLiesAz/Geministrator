import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
}

configurations.all {
    resolutionStrategy {
        dependencySubstitution {
            substitute(module("com.google.protobuf:protobuf-java"))
                .using(module("com.google.protobuf:protobuf-javalite:3.25.3"))
                .because("Android requires the javalite version of protobuf")

        }
        force("jakarta.annotation:jakarta.annotation-api:1.3.5")
        force("commons-logging:commons-logging:1.2")

        // Resolve duplicate classes
        exclude(group = "org.slf4j", module = "jcl-over-slf4j")
        exclude(group = "javax.annotation", module = "javax.annotation-api")
        exclude(group = "org.glassfish.hk2.external", module = "jakarta.inject")
    }
}

android {
    namespace = "com.hereliesaz.geministrator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hereliesaz.geministrator"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {

            abiFilters.addAll(listOf("x86_64", "arm64-v8a"))
        }
        manifestPlaceholders["appAuthRedirectScheme"] = "com.hereliesaz.geministrator.oauth2redirect"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        buildConfigField("String", "GITHUB_CLIENT_ID", "\"${localProperties.getProperty("github.clientId")}\"")
        buildConfigField("String", "GITHUB_CLIENT_SECRET", "\"${localProperties.getProperty("github.clientSecret")}\"")
    }


    buildFeatures {
        buildConfig = true
    }

    secrets {
        // Use the default properties file (local.properties)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),

                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"

            excludes += "META-INF/beans.xml"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "mozilla/public-suffix-list.txt"
        }
    }

}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":jules-api-client"))
    implementation(project(":github-api-client"))

    // Core & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Compose Bill of Materials
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.material.icons.extended)

    // Material 3 and Adaptive Layouts
    implementation(libs.androidx.material3)


    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // On-device Git
    implementation(libs.eclipse.jgit)


    // Debugging
    debugImplementation(libs.androidx.compose.ui.tooling)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // DataStore for settings
    implementation(libs.androidx.datastore.preferences)

    // DocumentFile for Storage Access Framework helpers
    implementation(libs.androidx.documentfile)

    // Sora Editor
    // implementation(platform(libs.sora.editor.bom))
    // implementation(libs.sora.editor)
    // implementation(libs.sora.language.textmate)

    // AzNavRail
    implementation(libs.aznavrail)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Agent Development Kit
    implementation(libs.google.adk)
    ksp(libs.google.adk) // This allows Hilt to see the ADK classes
    implementation(libs.generativeai)

    // A2A Communication
    implementation(libs.a2a.sdk.client)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Testing

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}