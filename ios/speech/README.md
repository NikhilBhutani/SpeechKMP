# ios/speech

> **Status: planned**

Swift Package wrapping the XCFramework produced by [`kotlin/speech`](../../kotlin/speech) with
idiomatic Swift APIs for iOS-only consumers who do not use Kotlin Multiplatform.

## What it will provide

- `SpeechRecognizer` and `SpeechSynthesizer` Swift types with `async`/`await` interfaces
- Combine publishers for streaming transcription results
- Distributed via Swift Package Index

## Installation

```swift
// Package.swift
.package(url: "https://github.com/deviceai-labs/deviceai", from: "0.1.0")
```

## Contributing

Implementation PRs welcome. The package embeds the static XCFramework built from `:kotlin:speech`
and exposes a thin Swift layer on top.
