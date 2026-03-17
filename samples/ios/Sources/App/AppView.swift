import SwiftUI
import ComposableArchitecture

struct AppView: View {
    let store: StoreOf<AppFeature>

    var body: some View {
        switch store.state {
        case .launch:
            if let store = store.scope(state: \.launch, action: \.launch) {
                LaunchView(store: store)
                    .transition(.opacity)
            }
        case .main:
            if let store = store.scope(state: \.main, action: \.main) {
                MainView(store: store)
                    .transition(.opacity)
            }
        }
    }
}
