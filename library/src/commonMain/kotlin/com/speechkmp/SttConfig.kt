package com.speechkmp

/**
 * Configuration for speech-to-text.
 */
data class SttConfig(
    /**
     * Language code (ISO 639-1). Examples: "en", "es", "fr", "de", "zh"
     * Use "auto" for automatic language detection.
     */
    val language: String = "en",

    /**
     * If true, translate non-English speech to English.
     */
    val translateToEnglish: Boolean = false,

    /**
     * Number of CPU threads for inference.
     */
    val maxThreads: Int = 4,

    /**
     * Use GPU acceleration if available (Metal on iOS/macOS).
     */
    val useGpu: Boolean = true,

    /**
     * Enable voice activity detection to skip silence.
     */
    val useVad: Boolean = true
)
