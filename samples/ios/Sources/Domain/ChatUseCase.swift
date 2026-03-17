import ComposableArchitecture

/// Encapsulates LLM chat: send messages, stream tokens, cancel, clear history.
/// The concrete implementation (session lifecycle, model caching) lives in the Data layer.
struct ChatUseCase: Sendable {
    var send:   @Sendable (String, String) async -> AsyncThrowingStream<String, Error>
    var cancel: @Sendable () -> Void         // synchronous — fire-and-forget
    var clear:  @Sendable () async -> Void
}

extension DependencyValues {
    var chatUseCase: ChatUseCase {
        get { self[ChatUseCase.self] }
        set { self[ChatUseCase.self] = newValue }
    }
}
