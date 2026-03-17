import ComposableArchitecture
import AVFoundation
import DeviceAiStt

// MARK: - Dependency

struct SpeechClient: Sendable {
    var startRecording:  @Sendable () async -> Void
    var stopRecording:   @Sendable () async -> [Float]
    var transcribe:      @Sendable ([Float]) async throws -> TranscriptionResult
}

extension SpeechClient: DependencyKey {
    static let liveValue: SpeechClient = {
        let recorder = AudioRecorder()
        let session  = try! DeviceAI.stt.session(
            modelPath: WhisperCatalog.tiny.localPathString
        )

        return SpeechClient(
            startRecording:  { await recorder.start() },
            stopRecording:   { await recorder.stop() },
            transcribe:      { samples in try await session.transcribe(samples: samples) }
        )
    }()

    static let previewValue = SpeechClient(
        startRecording: { },
        stopRecording:  { Array(repeating: 0, count: 16_000) },
        transcribe:     { _ in
            TranscriptionResult(
                text: "Preview transcription — runs fully on-device with Whisper Tiny.",
                segments: [],
                language: "en",
                durationMs: 1000
            )
        }
    )
}

extension DependencyValues {
    var speechClient: SpeechClient {
        get { self[SpeechClient.self] }
        set { self[SpeechClient.self] = newValue }
    }
}

// MARK: - Helpers

private extension WhisperModelInfo {
    var localPathString: String {
        DeviceAI.stt.modelManager.localPath(for: self).path
    }
}

// MARK: - Audio recorder (AVAudioEngine-based, 16 kHz mono float)

private actor AudioRecorder {
    private var engine:      AVAudioEngine?
    private var samples:     [Float] = []
    private let sampleRate:  Double  = 16_000

    func start() {
        samples = []
        let eng  = AVAudioEngine()
        engine   = eng
        let input = eng.inputNode
        let fmt   = AVAudioFormat(
            commonFormat: .pcmFormatFloat32,
            sampleRate:   sampleRate,
            channels:     1,
            interleaved:  false
        )!

        input.installTap(onBus: 0, bufferSize: 4096, format: fmt) { [weak self] buf, _ in
            guard let ch = buf.floatChannelData?[0] else { return }
            let frame = Array(UnsafeBufferPointer(start: ch, count: Int(buf.frameLength)))
            Task { await self?.append(frame) }
        }

        try? eng.start()
    }

    func stop() -> [Float] {
        engine?.inputNode.removeTap(onBus: 0)
        engine?.stop()
        engine = nil
        return samples
    }

    private func append(_ frame: [Float]) { samples.append(contentsOf: frame) }
}
