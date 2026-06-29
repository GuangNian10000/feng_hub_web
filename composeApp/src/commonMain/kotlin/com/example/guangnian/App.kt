package com.example.guangnian

import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.guangnian.di.appModule
import com.example.guangnian.feature.console.ui.InteractionScreen
import com.example.guangnian.feature.main.ui.MainControlScreen
import org.koin.compose.KoinApplication

// 定义应用层级路由枚举。
enum class AppRoute {
    MAIN_CONTROL,
    INTERACTION
}

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        var currentRoute by remember { mutableStateOf(AppRoute.MAIN_CONTROL) }

        MaterialTheme(colorScheme = darkColorScheme()) {
            // 使用 Crossfade 增加切换时的渐变动画过渡。
            Crossfade(targetState = currentRoute, label = "AppRoute") { route ->
                when (route) {
                    AppRoute.MAIN_CONTROL -> {
                        MainControlScreen(
                            onNavigateToInteraction = { currentRoute = AppRoute.INTERACTION }
                        )
                    }
                    AppRoute.INTERACTION -> {
                        InteractionScreen(
                            onBackClick = { currentRoute = AppRoute.MAIN_CONTROL }
                        )
                    }
                }
            }
        }
    }
}
