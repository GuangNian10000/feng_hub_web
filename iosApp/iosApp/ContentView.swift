import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // 键盘弹出时依然处理安全区
            .ignoresSafeArea(.all, edges: .all) // UI 铺满全屏
            .statusBarHidden(true) // 核心：隐藏顶部的通知状态栏
    }
}



