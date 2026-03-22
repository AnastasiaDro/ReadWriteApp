import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    IncomingDeckArchiveBridgeKt.handleIncomingDeckArchiveUrl(url: url.absoluteString)
                }
        }
    }
}
