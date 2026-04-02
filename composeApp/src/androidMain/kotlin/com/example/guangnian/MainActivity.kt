package com.example.guangnian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 设置内容区域延伸到系统窗口（全屏）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. 控制状态栏和导航栏隐藏
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 设置为“滑动边缘可短暂显示系统栏”的沉浸模式 (适合控制类 App)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // 隐藏状态栏和底部导航栏
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}