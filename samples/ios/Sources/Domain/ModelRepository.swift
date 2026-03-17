import Foundation

// MARK: - App-level model descriptors (no SDK types)

struct AppSttModel: Equatable, Identifiable, Sendable {
    let id: String
    let displayName: String
    let sizeBytes: Int64

    var formattedSize: String { Self.format(sizeBytes) }

    private static func format(_ bytes: Int64) -> String {
        let mb = Double(bytes) / 1_048_576
        return mb >= 1024 ? String(format: "%.1f GB", mb / 1024) : String(format: "%.0f MB", mb)
    }
}

struct AppLlmModel: Equatable, Identifiable, Sendable {
    let id: String
    let displayName: String
    let sizeBytes: Int64
    let parameters: String
    let quantization: String
    let description: String

    var formattedSize: String { Self.format(sizeBytes) }

    private static func format(_ bytes: Int64) -> String {
        let mb = Double(bytes) / 1_048_576
        return mb >= 1024 ? String(format: "%.1f GB", mb / 1024) : String(format: "%.0f MB", mb)
    }
}

// MARK: - Repository protocol

/// Encapsulates all model catalogue + download + selection operations.
/// Uses only app-level types — no SDK imports in features or views.
protocol ModelRepository: Sendable {
    func allSttModels() -> [AppSttModel]
    func allLlmModels() -> [AppLlmModel]
    func isSttDownloaded(_ id: String) async -> Bool
    func isLlmDownloaded(_ id: String) async -> Bool
    /// Stream of progress fractions (0.0–1.0).
    func downloadStt(_ id: String) -> AsyncThrowingStream<Double, Error>
    func downloadLlm(_ id: String) -> AsyncThrowingStream<Double, Error>
    func localSttPath(_ id: String) -> String
    func localLlmPath(_ id: String) -> String
}
