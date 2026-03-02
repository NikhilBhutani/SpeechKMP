# ios/speech

> **Status: planned**

Swift Package wrapping the XCFramework produced by [`kmp/speech`](../../kmp/speech) with
idiomatic Swift APIs for iOS-only consumers who do not use Kotlin Multiplatform.

## What it will provide

- `SpeechRecognizer` and `SpeechSynthesizer` Swift types with `async`/`await` interfaces
- Combine publishers for streaming transcription results
- Distributed via Swift Package Index

## Installation

```swift
// Package.swift
.package(url: "https://github.com/deviceai-labs/runtime-kmp", from: "0.2.0")
```

## Contributing

Implementation PRs welcome. The package embeds the static XCFramework built from `:kmp:speech`
and exposes a thin Swift layer on top.
