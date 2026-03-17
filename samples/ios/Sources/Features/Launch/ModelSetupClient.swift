import Foundation
import ComposableArchitecture
import DeviceAiStt
import DeviceAiLlm

// MARK: - Dependency

struct ModelSetupClient {
    var ensureReady: @Sendable (
        _ onSttProgress: @Sendable (Double) async -> Void,
        _ onLlmProgress: @Sendable (Double) async -> Void,
        _ onSuccess:     @Sendable () async -> Void,
        _ onFailure:     @Sendable (String) async -> Void
    ) async -> Void
}

extension ModelSetupClient: DependencyKey {
    static let liveValue = ModelSetupClient { onSttProgress, onLlmProgress, onSuccess, onFailure in
        do {
            // STT — Whisper Tiny
            let sttManager = DeviceAI.stt.modelManager
            if await !sttManager.isDownloaded(WhisperCatalog.tiny) {
                for try await p in sttManager.download(WhisperCatalog.tiny) {
                    await onSttProgress(p.fractionCompleted)
                }
            }
            await onSttProgress(1)

            // LLM — SmolLM2 135M
            let llmManager = DeviceAI.llm.modelManager
            if await !llmManager.isDownloaded(LlmCatalog.smolLM2_135M) {
                for try await p in llmManager.download(LlmCatalog.smolLM2_135M) {
                    await onLlmProgress(p.fractionCompleted)
                }
            }
            await onLlmProgress(1)

            await onSuccess()
        } catch {
            await onFailure(error.localizedDescription)
        }
    }

    static let previewValue = ModelSetupClient { _, _, onSuccess, _ in
        try? await Task.sleep(for: .seconds(1))
        await onSuccess()
    }
}

extension DependencyValues {
    var modelSetup: ModelSetupClient {
        get { self[ModelSetupClient.self] }
        set { self[ModelSetupClient.self] = newValue }
    }
}
