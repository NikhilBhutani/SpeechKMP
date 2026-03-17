import SwiftUI

struct MainView: View {
    @State private var selectedTab: Tab = .speech
    @State private var isShowingModels = false

    let speechVM: SpeechViewModel
    let chatVM: ChatViewModel
    let modelsVM: ModelsViewModel

    enum Tab { case speech, chat }

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack {
                SpeechView(viewModel: speechVM)
                    .toolbar { modelsButton }
            }
            .tabItem {
                Label("Speech", systemImage: selectedTab == .speech ? "mic.fill" : "mic")
            }
            .tag(Tab.speech)

            NavigationStack {
                ChatView(viewModel: chatVM)
                    .toolbar { modelsButton }
            }
            .tabItem {
                Label("Chat", systemImage: "bubble.left.and.bubble.right")
            }
            .tag(Tab.chat)
        }
        .tint(AppTheme.accent)
        .sheet(isPresented: $isShowingModels) {
            ModelsView(viewModel: modelsVM)
        }
    }

    @ToolbarContentBuilder
    private var modelsButton: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                modelsVM.loadIfNeeded()
                isShowingModels = true
            } label: {
                Image(systemName: "cpu")
                    .symbolRenderingMode(.hierarchical)
                    .foregroundStyle(AppTheme.accent)
            }
        }
    }
}
