plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

group = "dev.deviceai"
version = (System.getenv("RELEASE_VERSION") ?: "0.1.0-SNAPSHOT")

android {
    namespace = "dev.deviceai.android.speech"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Future: wrap kmp/speech with Jetpack APIs (ViewModels, Lifecycle, etc.)
// dependencies {
//     api(project(":kmp:speech"))
// }
