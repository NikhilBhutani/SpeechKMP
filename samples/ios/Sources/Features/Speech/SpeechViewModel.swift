import SwiftUI

@Observable
@MainActor
final class SpeechViewModel {
    var modelPath: String? = nil
    var isRecording = false
    var isTranscribing = false
    var transcript = ""
    var segments: [AppTranscriptionResult.Segment] = []
    var errorMessage: String? = nil

    private let speech: any SpeechRepository
    private var transcribeTask: Task<Void, Never>?

    init(speech: any SpeechRepository) {
        self.speech = speech
    }

    func recordButtonTapped() {
        guard let modelPath else {
            errorMessage = "No STT model selected. Tap the cpu icon to download one."
            return
        }
        if isRecording {
            isRecording = false
            let path = modelPath
            transcribeTask = Task {
                let samples = await speech.stopRecording()
                guard !samples.isEmpty else { return }
                isTranscribing = true
                do {
                    let result = try await speech.transcribe(samples, modelPath: path)
                    transcript = result.text
                    segments = result.segments
                    isTranscribing = false
                } catch {
                    errorMessage = error.localizedDescription
                    isTranscribing = false
                }
            }
        } else {
            errorMessage = nil
            isRecording = true
            Task { await speech.startRecording() }
        }
    }

    func clearTapped() {
        transcript = ""
        segments = []
        errorMessage = nil
    }
}
