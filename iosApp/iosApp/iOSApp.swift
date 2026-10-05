import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Before any scene: the Koin graph, and the customer sync's background task handler,
        // which iOS requires to be registered before launch finishes.
        AppSetupKt.setUpApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}