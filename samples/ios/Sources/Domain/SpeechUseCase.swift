import ComposableArchitecture

// MARK: - Domain model

struct AppTranscriptionResult: Equatable, Sendable {
    let text: String
    let segments: [Segment]
    let language: String
    let durationMs: Int64

    struct Segment: Equatable, Sendable {
        let text: String
        let startMs: Int64
        let endMs: Int64
    }
}

// MARK: - Use case

/// Encapsulates all audio recording + on-device transcription operations.
/// The concrete implementation lives in the Data layer; features only see this struct.
struct SpeechUseCase: Sendable {
    var startRecording: @Sendable () async -> Void
    var stopRecording:  @Sendable () async -> [Float]
    var transcribe:     @Sendable ([Float], String) async throws -> AppTranscriptionResult
}

extension DependencyValues {
    var speechUseCase: SpeechUseCase {
        get { self[SpeechUseCase.self] }
        set { self[SpeechUseCase.self] = newValue }
    }
}
