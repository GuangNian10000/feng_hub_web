package com.example.guangnian

import androidx.compose.animation.Crossfade
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.guangnian.di.appModule
import com.example.guangnian.feature.console.ui.InteractionScreen
import com.example.guangnian.feature.main.ui.MainControlScreen
import org.koin.compose.KoinApplication

import com.example.guangnian.feature.face.ui.FaceScreen

// 定义应用层级路由枚举。
enum class AppRoute {
    MAIN_CONTROL,
    INTERACTION,
    FACE_MANAGEMENT
}

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        var currentRoute by remember { mutableStateOf(AppRoute.MAIN_CONTROL) }

        MaterialTheme(colors = darkColors()) {
            // 使用 Crossfade 增加切换时的渐变动画过渡。
            Crossfade(targetState = currentRoute, label = "AppRoute") { route ->
                when (route) {
                    AppRoute.MAIN_CONTROL -> {
                        MainControlScreen(
                            onNavigateToInteraction = { currentRoute = AppRoute.INTERACTION },
                            onNavigateToFace = { currentRoute = AppRoute.FACE_MANAGEMENT }
                        )
                    }
                    AppRoute.INTERACTION -> {
                        InteractionScreen(
                            onBackClick = { currentRoute = AppRoute.MAIN_CONTROL }
                        )
                    }
                    AppRoute.FACE_MANAGEMENT -> {
                        FaceScreen(
                            onBack = { currentRoute = AppRoute.MAIN_CONTROL }
                        )
                    }
                }
            }
        }
    }
}
