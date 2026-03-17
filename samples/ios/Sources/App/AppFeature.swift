import ComposableArchitecture

/// Root feature — owns launch vs main navigation state.
@Reducer
struct AppFeature {

    @ObservableState
    enum State: Equatable {
        case launch(LaunchFeature.State)
        case main(MainFeature.State)

        init() { self = .launch(LaunchFeature.State()) }
    }

    enum Action {
        case launch(LaunchFeature.Action)
        case main(MainFeature.Action)
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .launch(.delegate(.didFinishSetup)):
                state = .main(MainFeature.State())
                return .none
            case .launch, .main:
                return .none
            }
        }
        .ifCaseLet(\.launch, action: \.launch) { LaunchFeature() }
        .ifCaseLet(\.main,   action: \.main)   { MainFeature() }
    }
}
