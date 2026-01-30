# SpeechKMP

Kotlin Multiplatform library for on-device Speech-to-Text (STT) and Text-to-Speech (TTS).

## Features

- **Speech-to-Text (STT):** Convert audio to text using OpenAI's Whisper model via [whisper.cpp](https://github.com/ggerganov/whisper.cpp)
- **Text-to-Speech (TTS):** Generate natural speech from text using [Piper](https://github.com/rhasspy/piper) neural TTS
- **Privacy-first:** All processing happens on-device, no internet required
- **Cross-platform:** Android, iOS, Desktop (macOS, Linux, Windows)
- **Lightweight:** Minimal dependencies, optimized for mobile

## Supported Platforms

| Platform | Status |
|----------|--------|
| Android | ✅ |
| iOS | ✅ |
| macOS | ✅ |
| Linux | 🚧 |
| Windows | 🚧 |

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.speechkmp:library:0.1.0")
}
```

## Usage

### Speech-to-Text (STT)

```kotlin
// Initialize with Whisper model
val modelPath = SpeechBridge.getModelPath("whisper-tiny.bin")
SpeechBridge.initStt(modelPath, SttConfig(language = "en"))

// Transcribe audio file
val text = SpeechBridge.transcribe("/path/to/audio.wav")
println(text)

// Transcribe with detailed results
val result = SpeechBridge.transcribeDetailed("/path/to/audio.wav")
println("Text: ${result.text}")
result.segments.forEach { segment ->
    println("[${segment.startMs}ms - ${segment.endMs}ms] ${segment.text}")
}

// Streaming transcription
SpeechBridge.transcribeStream(audioSamples, object : SttStream {
    override fun onPartialResult(text: String) {
        println("Partial: $text")
    }
    override fun onFinalResult(result: TranscriptionResult) {
        println("Final: ${result.text}")
    }
    override fun onError(message: String) {
        println("Error: $message")
    }
})

// Cleanup
SpeechBridge.shutdownStt()
```

### Text-to-Speech (TTS)

```kotlin
// Initialize with Piper voice model
SpeechBridge.initTts(
    modelPath = "/path/to/voice.onnx",
    configPath = "/path/to/voice.json",
    config = TtsConfig(speechRate = 1.0f)
)

// Synthesize to audio samples
val samples = SpeechBridge.synthesize("Hello, world!")

// Synthesize to file
SpeechBridge.synthesizeToFile("Hello, world!", "/path/to/output.wav")

// Streaming synthesis
SpeechBridge.synthesizeStream("Hello, world!", object : TtsStream {
    override fun onAudioChunk(samples: ShortArray) {
        // Play audio chunk
    }
    override fun onComplete() {
        println("Synthesis complete")
    }
    override fun onError(message: String) {
        println("Error: $message")
    }
})

// Cleanup
SpeechBridge.shutdownTts()
```

## Models

### Whisper Models (STT)

Download from [Hugging Face](https://huggingface.co/ggerganov/whisper.cpp):

| Model | Size | Speed | Accuracy |
|-------|------|-------|----------|
| tiny | 75 MB | Fastest | Lower |
| base | 142 MB | Fast | Good |
| small | 466 MB | Medium | Better |
| medium | 1.5 GB | Slow | High |

### Piper Voices (TTS)

Download from [Hugging Face](https://huggingface.co/rhasspy/piper-voices):

| Voice | Size | Language |
|-------|------|----------|
| en_US-lessac-medium | 60 MB | English US |
| en_GB-alba-medium | 55 MB | English UK |
| de_DE-thorsten-medium | 65 MB | German |

## Building from Source

### Prerequisites

- CMake 3.22+
- Android NDK r26+ (for Android)
- Xcode 15+ (for iOS/macOS)
- Kotlin 2.0+

### Setup

1. Clone the repository with submodules:
```bash
git clone --recursive https://github.com/example/speechkmp.git
cd speechkmp
```

2. Add the native library submodules:
```bash
git submodule add https://github.com/ggerganov/whisper.cpp.git whisper.cpp
git submodule add https://github.com/rhasspy/piper.git piper
```

3. Build:
```bash
./gradlew build
```

## License

MIT License
