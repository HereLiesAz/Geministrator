plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.google.protobuf:protobuf-java"))
            .using(module("com.google.protobuf:protobuf-javalite:3.25.3"))
            .because("Android requires the javalite version of protobuf")
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
    implementation(libs.play.services.tasks)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.ai)
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
    implementation(platform(libs.sora.editor.bom))
    implementation(libs.sora.editor)
    implementation(libs.sora.language.textmate)

    // AzNavRail
    implementation(libs.aznavrail)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Gemini API
    implementation(libs.google.cloud.vertexai)

    // Agent Development Kit
    implementation(libs.google.adk)

    // Testing
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation("io.mockk:mockk-android:1.13.3")
    testImplementation(libs.junit)
}
