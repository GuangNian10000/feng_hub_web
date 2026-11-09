package com.example.guangnian.feature.update

actual object AppUpdater {
    actual fun checkUpdate() {
        // Web 平台始终通过浏览器访问最新部署版本，无需客户端 APK 更新机制
    }
}
