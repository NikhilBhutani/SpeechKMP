import DeviceAiCore
import DeviceAiStt
import DeviceAiLlm

// MARK: - Concrete implementation

final class LiveModelRepository: ModelRepository {

    func allSttModels() -> [AppSttModel] {
        WhisperCatalog.all.map { $0.toDomain() }
    }

    func allLlmModels() -> [AppLlmModel] {
        LlmCatalog.all.map { $0.toDomain() }
    }

    func isSttDownloaded(_ id: String) async -> Bool {
        guard let m = WhisperCatalog.all.first(where: { $0.id == id }) else { return false }
        return await DeviceAI.stt.modelManager.isDownloaded(m)
    }

    func isLlmDownloaded(_ id: String) async -> Bool {
        guard let m = LlmCatalog.all.first(where: { $0.id == id }) else { return false }
        return await DeviceAI.llm.modelManager.isDownloaded(m)
    }

    func downloadStt(_ id: String) -> AsyncThrowingStream<Double, Error> {
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
    }

    func downloadLlm(_ id: String) -> AsyncThrowingStream<Double, Error> {
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
    }

    func localSttPath(_ id: String) -> String {
        guard let m = WhisperCatalog.all.first(where: { $0.id == id }) else { return "" }
        return DeviceAI.stt.modelManager.localPath(for: m).path
    }

    func localLlmPath(_ id: String) -> String {
        guard let m = LlmCatalog.all.first(where: { $0.id == id }) else { return "" }
        return DeviceAI.llm.modelManager.localPath(for: m).path
    }
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
