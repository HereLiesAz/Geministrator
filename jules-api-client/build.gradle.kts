plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    constraints {
        implementation("com.google.protobuf:protobuf-java:3.25.8") {
            because("ADK requires protobuf-java, but a newer version is being pulled in transitively.")
        }
    }
    implementation(project(":github-api-client"))
    implementation(libs.google.cloud.vertexai)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.google.adk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
}