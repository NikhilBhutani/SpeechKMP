import Foundation
import ComposableArchitecture

// MARK: - App-level model descriptors (no SDK types)

struct AppSttModel: Equatable, Identifiable, Sendable {
    let id: String
    let displayName: String
    let sizeBytes: Int64

    var formattedSize: String { Self.format(sizeBytes) }
}

struct AppLlmModel: Equatable, Identifiable, Sendable {
    let id: String
    let displayName: String
    let sizeBytes: Int64
    let parameters: String
    let quantization: String
    let description: String

    var formattedSize: String { Self.format(sizeBytes) }
}

private extension AppSttModel {
    static func format(_ bytes: Int64) -> String {
        let mb = Double(bytes) / 1_048_576
        return mb >= 1024 ? String(format: "%.1f GB", mb / 1024) : String(format: "%.0f MB", mb)
    }
}

private extension AppLlmModel {
    static func format(_ bytes: Int64) -> String {
        let mb = Double(bytes) / 1_048_576
        return mb >= 1024 ? String(format: "%.1f GB", mb / 1024) : String(format: "%.0f MB", mb)
    }
}

// MARK: - Use case

/// Encapsulates all model catalogue + download + selection operations.
/// Uses only app-level types — no SDK imports in features or views.
struct ModelUseCase: Sendable {
    var allSttModels:    @Sendable () -> [AppSttModel]
    var allLlmModels:    @Sendable () -> [AppLlmModel]
    var isSttDownloaded: @Sendable (String) async -> Bool
    var isLlmDownloaded: @Sendable (String) async -> Bool
    /// Stream of progress fractions (0.0–1.0).
    var downloadStt:     @Sendable (String) -> AsyncThrowingStream<Double, Error>
    var downloadLlm:     @Sendable (String) -> AsyncThrowingStream<Double, Error>
    var localSttPath:    @Sendable (String) -> String
    var localLlmPath:    @Sendable (String) -> String
}

extension DependencyValues {
    var modelUseCase: ModelUseCase {
        get { self[ModelUseCase.self] }
        set { self[ModelUseCase.self] = newValue }
    }
}
