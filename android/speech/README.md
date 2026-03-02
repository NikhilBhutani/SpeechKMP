# android/speech

> **Status: planned**

Jetpack-idiomatic wrapper around [`kmp/speech`](../../kmp/speech) for Android-only consumers who prefer a
fully Android-native dependency (no KMP multiplatform setup required).

## What it will provide

- `SpeechViewModel` — Jetpack ViewModel wrapping `SpeechRecognizer` and `SpeechSynthesizer`
- Lifecycle-aware coroutine scopes (tied to `viewModelScope`)
- `Flow`-based result APIs matching Jetpack conventions
- Maven artifact: `dev.deviceai:android-speech`

## Dependency

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("dev.deviceai:android-speech:<version>")
}
```

## Contributing

Implementation PRs welcome. The module depends on `:kmp:speech` and adds a thin Jetpack layer on top.
