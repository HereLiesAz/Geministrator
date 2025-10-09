plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Retrofit for networking
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")


    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines for asynchronous programming
    implementation(libs.kotlinx.coroutines.core)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}