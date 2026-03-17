import ComposableArchitecture
import DeviceAiStt
import DeviceAiLlm

/// Manages the one-time setup screen: model existence check → optional download → ready.
@Reducer
struct LaunchFeature {

    @ObservableState
    struct State: Equatable {
        var phase: Phase = .checking
        var sttProgress: Double = 0       // 0–1
        var llmProgress: Double = 0       // 0–1

        enum Phase: Equatable {
            case checking
            case downloading
            case ready
            case failed(String)
        }
    }

    enum Action {
        case onAppear
        case sttProgressUpdated(Double)
        case llmProgressUpdated(Double)
        case setupSucceeded
        case setupFailed(String)
        case getStartedTapped
        case retryTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case didFinishSetup
        }
    }

    @Dependency(\.modelSetup) var modelSetup

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {

            case .onAppear, .retryTapped:
                state.phase       = .checking
                state.sttProgress = 0
                state.llmProgress = 0
                return .run { send in
                    await modelSetup.ensureReady(
                        onSttProgress: { p in await send(.sttProgressUpdated(p)) },
                        onLlmProgress: { p in await send(.llmProgressUpdated(p)) },
                        onSuccess:     { await send(.setupSucceeded) },
                        onFailure:     { await send(.setupFailed($0)) }
                    )
                }

            case .sttProgressUpdated(let p):
                state.sttProgress = p
                state.phase       = .downloading
                return .none

            case .llmProgressUpdated(let p):
                state.llmProgress = p
                state.phase       = .downloading
                return .none

            case .setupSucceeded:
                state.phase = .ready
                return .none

            case .setupFailed(let msg):
                state.phase = .failed(msg)
                return .none

            case .getStartedTapped:
                return .send(.delegate(.didFinishSetup))

            case .delegate:
                return .none
            }
        }
    }
}
