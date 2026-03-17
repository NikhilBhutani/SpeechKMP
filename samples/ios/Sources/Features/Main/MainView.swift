import SwiftUI
import ComposableArchitecture

struct MainView: View {
    @Bindable var store: StoreOf<MainFeature>

    var body: some View {
        TabView(selection: $store.selectedTab.sending(\.tabSelected)) {

            NavigationStack {
                SpeechView(store: store.scope(state: \.speech, action: \.speech))
            }
            .tabItem {
                Label("Speech", systemImage: store.selectedTab == .speech ? "mic.fill" : "mic")
            }
            .tag(MainFeature.State.Tab.speech)

            NavigationStack {
                ChatView(store: store.scope(state: \.chat, action: \.chat))
            }
            .tabItem {
                Label("Chat", systemImage: "bubble.left.and.bubble.right")
            }
            .tag(MainFeature.State.Tab.chat)
        }
        .tint(AppTheme.accent)
    }
}
