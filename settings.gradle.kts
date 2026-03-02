rootProject.name = "deviceai-runtime-kmp"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":kmp:core")
include(":kmp:speech")
include(":kmp:llm")
include(":samples:composeApp")
