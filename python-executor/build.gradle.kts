plugins {
    id("com.android.library") version "8.4.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
    id("com.chaquo.python")
}

android {
    namespace = "com.jules.pythonexecutor"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
}

chaquopy {
    defaultConfig {
        version = "3.11"
        sourceSets {
            getByName("main") {
                srcDir("src/main/python")
            }
        }
    }
}