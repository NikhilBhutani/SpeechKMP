import ComposableArchitecture
import DeviceAiLlm

// MARK: - Dependency

struct ChatClient: Sendable {
    var send:   @Sendable (String) async -> AsyncThrowingStream<String, Error>
    var cancel: @Sendable () async -> Void
    var clear:  @Sendable () async -> Void
}

extension ChatClient: DependencyKey {
    static let liveValue: ChatClient = {
        // Session is created once and reused across sends.
        let session = DeviceAI.llm.chat(
            modelPath: LlmCatalog.smolLM2_135M.localPathString
        ) {
            $0.systemPrompt = "You are a helpful on-device AI assistant. Be concise."
            $0.maxTokens    = 512
            $0.temperature  = 0.7
        }

        return ChatClient(
            send:   { text in await session.send(text) },
            cancel: { session.cancel() },
            clear:  { await session.clearHistory() }
        )
    }()

    static let previewValue = ChatClient(
        send: { text in
            AsyncThrowingStream { c in
                Task {
                    let words = "I'm a preview response for: \(text)".split(separator: " ")
                    for word in words {
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

extension DependencyValues {
    var chatClient: ChatClient {
        get { self[ChatClient.self] }
        set { self[ChatClient.self] = newValue }
    }
}

// MARK: - Helpers

private extension LlmModelInfo {
    /// Convenience — path as String for ChatSession init.
    var localPathString: String {
        DeviceAI.llm.modelManager.localPath(for: self).path
    }
}
