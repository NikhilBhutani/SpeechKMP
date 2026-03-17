import Foundation

// MARK: - Domain model

struct AppTranscriptionResult: Equatable, Sendable {
    let text: String
    let segments: [Segment]
    let language: String
    let durationMs: Int64

    struct Segment: Equatable, Identifiable, Sendable {
        let id = UUID()
        let text: String
        let startMs: Int64
        let endMs: Int64
    }
}

// MARK: - Repository protocol

/// Encapsulates all audio recording + on-device transcription operations.
/// The concrete implementation lives in the Data layer; ViewModels only see this protocol.
protocol SpeechRepository: Sendable {
    func startRecording() async
    func stopRecording() async -> [Float]
    func transcribe(_ samples: [Float], modelPath: String) async throws -> AppTranscriptionResult
}
