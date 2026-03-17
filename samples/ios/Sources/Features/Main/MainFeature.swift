import ComposableArchitecture

@Reducer
struct MainFeature {

    @ObservableState
    struct State: Equatable {
        var selectedTab: Tab = .speech
        var speech = SpeechFeature.State()
        var chat   = ChatFeature.State()

        enum Tab: Equatable { case speech, chat }
    }

    enum Action {
        case tabSelected(State.Tab)
        case speech(SpeechFeature.Action)
        case chat(ChatFeature.Action)
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .tabSelected(let tab):
                state.selectedTab = tab
                return .none
            case .speech, .chat:
                return .none
            }
        }
        Scope(state: \.speech, action: \.speech) { SpeechFeature() }
        Scope(state: \.chat,   action: \.chat)   { ChatFeature() }
    }
}
