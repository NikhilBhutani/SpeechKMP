import SwiftUI
import ComposableArchitecture
import DeviceAiCore

@main
struct DeviceAILabsApp: App {

    static let store = Store(initialState: AppFeature.State()) {
        AppFeature()
    }

    init() {
        DeviceAI.configure()
    }

    var body: some Scene {
        WindowGroup {
            AppView(store: DeviceAILabsApp.store)
        }
    }
}
