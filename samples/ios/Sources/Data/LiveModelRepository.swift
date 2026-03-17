import ComposableArchitecture
import DeviceAiCore
import DeviceAiStt
import DeviceAiLlm

// MARK: - DependencyKey (Data → Domain bridge)

extension ModelUseCase: DependencyKey {
    static let liveValue = ModelUseCase(
        allSttModels: { WhisperCatalog.all.map { $0.toDomain() } },
        allLlmModels: { LlmCatalog.all.map { $0.toDomain() } },

        isSttDownloaded: { id in
            guard let m = WhisperCatalog.all.first(where: { $0.id == id }) else { return false }
            return await DeviceAI.stt.modelManager.isDownloaded(m)
        },
        isLlmDownloaded: { id in
            guard let m = LlmCatalog.all.first(where: { $0.id == id }) else { return false }
            return await DeviceAI.llm.modelManager.isDownloaded(m)
        },

        downloadStt: { id in
            guard let m = WhisperCatalog.all.first(where: { $0.id == id }) else {
                return AsyncThrowingStream { $0.finish(throwing: ModelLookupError.notFound(id)) }
            }
            let stream = DeviceAI.stt.modelManager.download(m)
            return AsyncThrowingStream { continuation in
                Task {
                    do {
                        for try await p in stream { continuation.yield(p.fraction ?? 0) }
                        continuation.finish()
                    } catch { continuation.finish(throwing: error) }
                }
            }
        },
        downloadLlm: { id in
            guard let m = LlmCatalog.all.first(where: { $0.id == id }) else {
                return AsyncThrowingStream { $0.finish(throwing: ModelLookupError.notFound(id)) }
            }
            let stream = DeviceAI.llm.modelManager.download(m)
            return AsyncThrowingStream { continuation in
                Task {
                    do {
                        for try await p in stream { continuation.yield(p.fraction ?? 0) }
                        continuation.finish()
                    } catch { continuation.finish(throwing: error) }
                }
            }
        },

        localSttPath: { id in
            guard let m = WhisperCatalog.all.first(where: { $0.id == id }) else { return "" }
            return DeviceAI.stt.modelManager.localPath(for: m).path
        },
        localLlmPath: { id in
            guard let m = LlmCatalog.all.first(where: { $0.id == id }) else { return "" }
            return DeviceAI.llm.modelManager.localPath(for: m).path
        }
    )

    static let previewValue = ModelUseCase(
        allSttModels:    { WhisperCatalog.all.map { $0.toDomain() } },
        allLlmModels:    { LlmCatalog.all.map { $0.toDomain() } },
        isSttDownloaded: { _ in false },
        isLlmDownloaded: { _ in false },
        downloadStt:     { _ in AsyncThrowingStream { $0.finish() } },
        downloadLlm:     { _ in AsyncThrowingStream { $0.finish() } },
        localSttPath:    { _ in "" },
        localLlmPath:    { _ in "" }
    )
}

// MARK: - SDK → Domain mapping

private extension WhisperModelInfo {
    func toDomain() -> AppSttModel {
        AppSttModel(id: id, displayName: displayName, sizeBytes: sizeBytes)
    }
}

private extension LlmModelInfo {
    func toDomain() -> AppLlmModel {
        AppLlmModel(
            id: id, displayName: displayName, sizeBytes: sizeBytes,
            parameters: parameters, quantization: quantization, description: description
        )
    }
}

// MARK: - Errors

enum ModelLookupError: Error {
    case notFound(String)
}
