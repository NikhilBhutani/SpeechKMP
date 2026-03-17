import SwiftUI

@main
struct DeviceAILabsApp: App {
    @State private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            MainView(
                speechVM: container.speechVM,
                chatVM:   container.chatVM,
                modelsVM: container.modelsVM
            )
        }
    }
}
