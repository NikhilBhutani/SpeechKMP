import ComposableArchitecture
import DeviceAiLlm

// MARK: - DependencyKey (Data → Domain bridge)

extension ChatUseCase: DependencyKey {
    static let liveValue: ChatUseCase = {
        let cache = ChatSessionCache()

        return ChatUseCase(
            send: { text, modelPath in await cache.send(text, modelPath: modelPath) },
            cancel: { Task { await cache.cancel() } },
            clear:  { await cache.clearHistory() }
        )
    }()

    static let previewValue = ChatUseCase(
        send: { text, _ in
            AsyncThrowingStream { c in
                Task {
                    for word in "Preview response for: \(text)".split(separator: " ") {
                        try? await Task.sleep(for: .milliseconds(80))
                        c.yield(String(word) + " ")
                    }
                    c.finish()
                }
            }
        },
        cancel: { },
        clear:  { }
    )
}

// MARK: - Chat session cache

private actor ChatSessionCache {
    private var session: ChatSession?
    private var currentPath: String?

    private func getSession(for path: String) -> ChatSession {
        if currentPath == path, let s = session { return s }
        let s = DeviceAI.llm.chat(modelPath: path) {
            $0.systemPrompt = "You are a helpful on-device AI assistant. Be concise."
            $0.maxTokens    = 512
            $0.temperature  = 0.7
        }
        session     = s
        currentPath = path
        return s
    }

    func send(_ text: String, modelPath: String) -> AsyncThrowingStream<String, Error> {
        let s = getSession(for: modelPath)
        return AsyncThrowingStream { continuation in
            Task {
                do {
                    for try await token in await s.send(text) { continuation.yield(token) }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }

    func cancel() { session?.cancel() }
    func clearHistory() async { await session?.clearHistory() }
}
