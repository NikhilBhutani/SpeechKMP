import ComposableArchitecture

@Reducer
struct ChatFeature {

    @ObservableState
    struct State: Equatable {
        /// Set by MainFeature when the user selects an LLM model in the Models sheet.
        var modelPath: String? = nil
        var messages: [ChatMessage] = []
        var inputText: String = ""
        var isGenerating: Bool = false
        var streamingText: String = ""
        var errorMessage: String? = nil
    }

    enum Action: BindableAction {
        case binding(BindingAction<State>)
        case sendTapped
        case tokenReceived(String)
        case generationFinished
        case generationFailed(String)
        case clearTapped
        case cancelTapped
    }

    @Dependency(\.chatUseCase) var chatUseCase

    var body: some ReducerOf<Self> {
        BindingReducer()

        Reduce { state, action in
            switch action {

            case .sendTapped:
                let text = state.inputText.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !text.isEmpty, !state.isGenerating else { return .none }
                guard let modelPath = state.modelPath else {
                    state.errorMessage = "No LLM model selected. Tap the \u{24B8} icon to download one."
                    return .none
                }

                state.messages.append(ChatMessage(role: .user, text: text))
                state.inputText     = ""
                state.isGenerating  = true
                state.streamingText = ""
                state.errorMessage  = nil

                return .run { [modelPath] send in
                    do {
                        for try await token in await chatUseCase.send(text, modelPath) {
                            await send(.tokenReceived(token))
                        }
                        await send(.generationFinished)
                    } catch {
                        await send(.generationFailed(error.localizedDescription))
                    }
                }
                .cancellable(id: CancelID.generation, cancelInFlight: true)

            case .tokenReceived(let token):
                state.streamingText += token
                return .none

            case .generationFinished:
                if !state.streamingText.isEmpty {
                    state.messages.append(ChatMessage(role: .assistant, text: state.streamingText))
                }
                state.streamingText = ""
                state.isGenerating  = false
                return .none

            case .generationFailed(let msg):
                state.errorMessage  = msg
                state.streamingText = ""
                state.isGenerating  = false
                return .none

            case .clearTapped:
                state.messages      = []
                state.streamingText = ""
                state.errorMessage  = nil
                return .run { _ in await chatUseCase.clear() }

            case .cancelTapped:
                state.isGenerating  = false
                state.streamingText = ""
                chatUseCase.cancel()
                return .cancel(id: CancelID.generation)

            case .binding:
                return .none
            }
        }
    }

    private enum CancelID { case generation }
}

// MARK: - Presentation model

struct ChatMessage: Equatable, Identifiable {
    let id = UUID()
    let role: Role
    let text: String

    enum Role: Equatable { case user, assistant }
}
