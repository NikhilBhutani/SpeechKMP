import ComposableArchitecture
import AVFoundation
import DeviceAiStt

// MARK: - DependencyKey (Data → Domain bridge)

extension SpeechUseCase: DependencyKey {
    static let liveValue: SpeechUseCase = {
        let recorder = AudioRecorder()
        let cache    = SttSessionCache()

        return SpeechUseCase(
            startRecording: { await recorder.start() },
            stopRecording:  { await recorder.stop() },
            transcribe: { samples, path in
                let session = try await cache.session(for: path)
                let result  = try await session.transcribe(samples: samples)
                return result.toDomain()
            }
        )
    }()

    static let previewValue = SpeechUseCase(
        startRecording: { },
        stopRecording:  { Array(repeating: 0, count: 16_000) },
        transcribe: { _, _ in
            AppTranscriptionResult(
                text: "Preview transcription — runs on-device with Whisper.",
                segments: [], language: "en", durationMs: 1000
            )
        }
    )
}

// MARK: - STT session cache

private actor SttSessionCache {
    private var session: SttSession?
    private var currentPath: String?

    func session(for path: String) throws -> SttSession {
        if currentPath == path, let s = session { return s }
        let s = try DeviceAI.stt.session(modelPath: path)
        session     = s
        currentPath = path
        return s
    }
}

// MARK: - Audio recorder (AVAudioEngine, 16 kHz mono float32)

private actor AudioRecorder {
    private var engine:     AVAudioEngine?
    private var samples:    [Float] = []
    private let sampleRate: Double  = 16_000

    func start() {
        samples = []
        let eng   = AVAudioEngine()
        engine    = eng
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

// MARK: - SDK → Domain mapping

private extension TranscriptionResult {
    func toDomain() -> AppTranscriptionResult {
        AppTranscriptionResult(
            text: text,
            segments: segments.map {
                .init(text: $0.text, startMs: $0.startMs, endMs: $0.endMs)
            },
            language: language,
            durationMs: durationMs
        )
    }
}
