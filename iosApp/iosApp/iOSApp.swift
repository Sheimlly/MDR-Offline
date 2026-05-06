import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        KoinInitializerKt.doInitKoin()
        UNUserNotificationCenter.current().delegate = NotificationDelegate.shared
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
           if let error = error {
               print("Notification permission error: \(error)")
           } else {
               print("Notification permission granted: \(granted)")
           }
       }
        ComposeApp.DownloadManager_iosKt.downloadServiceProvider = DownloadService()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}


func requestNotificationPermission() {
    let center = UNUserNotificationCenter.current()
    center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
        if let error = error {
            print("Notification permission error: \(error.localizedDescription)")
            return
        }
        if granted {
            print("Notification permission granted")
        } else {
            print("Notification permission denied")
        }
    }
}


// AppDelegate for background events
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        print("Reconnecting background URLSession: \(identifier)")
        BackgroundDownloadService.shared.handleEventsForBackgroundSession(
            identifier: identifier,
            completionHandler: completionHandler
        )
    }
}
