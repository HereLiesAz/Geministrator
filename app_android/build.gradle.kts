plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hereliesaz.geministrator.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hereliesaz.geministrator.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // This version is aligned with the project's Kotlin version (1.9.23)
        kotlinCompilerExtensionVersion = "2.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Project Modules
    implementation(project(":cli"))

    // Core & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)

    // Compose Bill of Materials
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)

    // Material 3 and Adaptive Layouts
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window)
            implementation(libs.androidx.material3.adaptive)
            implementation(libs.androidx.material3.adaptive.navigation.suite)

            // Navigation
            implementation(libs.androidx.navigation.compose)

            // ViewModel
            implementation(libs.androidx.lifecycle.viewmodel.compose)

            // On-device Git
            implementation(libs.eclipse.jgit)

            // Testing
            testImplementation(libs.junit)
            androidTestImplementation(libs.androidx.test.ext.junit)
            androidTestImplementation(libs.androidx.test.espresso.core)
            androidTestImplementation(platform(libs.androidx.compose.bom))
            androidTestImplementation(libs.androidx.compose.ui.test.junit4)

            // Debugging
            debugImplementation(libs.androidx.compose.ui.tooling)
            debugImplementation(libs.androidx.compose.ui.test.manifest)
}