import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {

    init() {
        // Só configura se o GoogleService-Info.plist estiver no bundle. Ele é gitignored
        // — o repositório é público —, e `configure()` sem o arquivo derruba o app na
        // abertura. Sem ele o app sobe e só o login fica indisponível.
        if Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil {
            FirebaseApp.configure()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
