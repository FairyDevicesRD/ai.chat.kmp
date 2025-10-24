import SwiftUI
import shared

class AppGraphHolder: ObservableObject {
    let graph: AppGraph

    init() {
        self.graph = AppGraphStore.shared.createAppGraph()
        print("AppGraph INITIALIZED: \(self.graph)")
    }
}

@main
struct iOSApp: App {
    @StateObject private var graphHolder = AppGraphHolder()

    init() {
        Logger_iosKt.doInitLogger()
    }
    var body: some Scene {
        WindowGroup {
            ContentView(graph: graphHolder.graph)
        }
    }
}
