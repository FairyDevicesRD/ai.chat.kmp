import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    let graph: AppGraph

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController(appGraph: graph)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}


struct ContentView: View {
    let graph: AppGraph

    var body: some View {
        ComposeView(graph: graph)
            .ignoresSafeArea()
    }
}
