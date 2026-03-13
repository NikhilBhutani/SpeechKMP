package dev.deviceai.models

/**
 * Returns the platform-appropriate Whisper download strategy.
 * On iOS this is a subclass that also fetches the Core ML encoder bundle.
 * On all other platforms this is the standard [WhisperDownloadStrategy].
 */
internal expect fun createWhisperDownloadStrategy(
    http: HttpFileDownloader,
    fs: FileSystem,
    paths: StoragePaths,
    store: MetadataStore
): ModelDownloadStrategy
