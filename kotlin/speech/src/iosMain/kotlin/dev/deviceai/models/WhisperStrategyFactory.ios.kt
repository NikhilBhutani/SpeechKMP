package dev.deviceai.models

import dev.deviceai.native.dai_extract_zip
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

private const val WHISPER_HF_BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main"

/**
 * iOS actual: returns [IosWhisperDownloadStrategy] which also downloads
 * the pre-compiled Core ML encoder bundle for ANE acceleration.
 */
internal actual fun createWhisperDownloadStrategy(
    http: HttpFileDownloader,
    fs: FileSystem,
    paths: StoragePaths,
    store: MetadataStore
): ModelDownloadStrategy = IosWhisperDownloadStrategy(http, fs, paths, store)

/**
 * iOS-specific extension of [WhisperDownloadStrategy] that also fetches
 * `<base>-encoder.mlmodelc.zip` from HuggingFace and extracts it alongside
 * the GGUF so whisper.cpp finds the Core ML bundle at runtime.
 *
 * This class lives entirely in iosMain — commonMain has zero knowledge of Core ML.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosWhisperDownloadStrategy(
    http: HttpFileDownloader,
    fs: FileSystem,
    paths: StoragePaths,
    store: MetadataStore
) : WhisperDownloadStrategy(http, fs, paths, store) {

    override suspend fun download(
        model: ModelInfo,
        onProgress: (DownloadProgress) -> Unit
    ): LocalModel {
        // Download the GGUF first (standard path)
        val localModel = super.download(model, onProgress)

        // Then download and extract the Core ML encoder bundle
        val modelDir = localModel.modelPath.substringBeforeLast('/')
        downloadAndExtractCoreMlEncoder(model.id, modelDir)

        return localModel
    }

    private suspend fun downloadAndExtractCoreMlEncoder(modelId: String, modelDir: String) {
        // "ggml-tiny.en.bin" -> "ggml-tiny.en-encoder.mlmodelc"
        val base = modelId.substringBeforeLast('.')
        val encoderDir = "$modelDir/$base-encoder.mlmodelc"

        // Skip if already extracted
        if (NSFileManager.defaultManager.fileExistsAtPath(encoderDir)) return

        val zipName = "$base-encoder.mlmodelc.zip"
        val zipPath = "$modelDir/$zipName"

        http.download(url = "$WHISPER_HF_BASE/$zipName", destPath = zipPath)
        dai_extract_zip(zipPath, modelDir)
        NSFileManager.defaultManager.removeItemAtPath(zipPath, null)
    }
}
