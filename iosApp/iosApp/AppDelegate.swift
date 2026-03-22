import UIKit
import ComposeApp

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        IncomingDeckArchiveBridgeKt.handleIncomingDeckArchiveUrl(url: url.absoluteString)
        return true
    }
}
