dependencies {
    // Retrofit for networking
    implementation(libs.retrofit)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.okhttp)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines for asynchronous programming
    implementation(libs.kotlinx.coroutines.core)

    // Gemini API
    api(libs.google.cloud.vertexai)

    // A2A Communication
    implementation(libs.a2a.sdk.client)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
