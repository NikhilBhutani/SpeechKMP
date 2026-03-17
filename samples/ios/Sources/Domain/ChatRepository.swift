// MARK: - Repository protocol

/// Encapsulates LLM chat: send messages, stream tokens, cancel, clear history.
/// The concrete implementation (session lifecycle, model caching) lives in the Data layer.
protocol ChatRepository: Sendable {
    func send(_ text: String, modelPath: String) async -> AsyncThrowingStream<String, Error>
    func cancel()
    func clearHistory() async
}
