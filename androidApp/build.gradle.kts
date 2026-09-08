plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.hereliesaz.geministrator"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hereliesaz.geministrator"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "2.0.0-alpha01"
    }

    val ciKeystorePath = System.getenv("GEMINISTRATOR_KEYSTORE_PATH")
    val ciKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val ciKeyAlias = System.getenv("KEY_ALIAS")
    val ciKeyPassword = System.getenv("KEY_PASSWORD")

    if (
        !ciKeystorePath.isNullOrBlank() &&
        !ciKeystorePassword.isNullOrBlank() &&
        !ciKeyAlias.isNullOrBlank() &&
        !ciKeyPassword.isNullOrBlank()
    ) {
        signingConfigs {
            create("ciRelease") {
                storeFile = file(ciKeystorePath)
                storePassword = ciKeystorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("ciRelease")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
}
