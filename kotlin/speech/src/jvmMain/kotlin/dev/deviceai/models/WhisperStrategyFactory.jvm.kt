package dev.deviceai.models

internal actual fun createWhisperDownloadStrategy(
    http: HttpFileDownloader,
    fs: FileSystem,
    paths: StoragePaths,
    store: MetadataStore
): ModelDownloadStrategy = WhisperDownloadStrategy(http, fs, paths, store)
