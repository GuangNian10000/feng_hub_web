package com.example.guangnian

import androidx.compose.animation.Crossfade
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.example.guangnian.di.appModule
import com.example.guangnian.feature.update.AppUpdater
import com.example.guangnian.feature.blue.BleManagementScreen
import com.example.guangnian.feature.console.ui.InteractionScreen
import com.example.guangnian.feature.main.ui.MainControlScreen
import com.example.guangnian.feature.remote.RemoteDesktopScreen
import org.koin.compose.KoinApplication

import com.example.guangnian.feature.face.ui.FaceScreen
import com.example.guangnian.feature.map.MapEditScreen
import com.example.guangnian.feature.console.data.bean.MapBean

// 定义应用层级路由枚举。
enum class AppRoute {
    MAIN_CONTROL,
    INTERACTION,
    FACE_MANAGEMENT,
    MAP_EDIT,
    BLE_MANAGEMENT,
    REMOTE_DESKTOP
}

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        var currentRoute by remember { mutableStateOf(AppRoute.MAIN_CONTROL) }
        var editingMap by remember { mutableStateOf<MapBean?>(null) }
        var remoteDesktopIp by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            AppUpdater.checkUpdate()
        }

        MaterialTheme(colors = darkColors()) {
            // 使用 Crossfade 增加切换时的渐变动画过渡。
            Crossfade(targetState = currentRoute, label = "AppRoute") { route ->
                when (route) {
                    AppRoute.MAIN_CONTROL -> {
                        MainControlScreen(
                            onNavigateToInteraction = { currentRoute = AppRoute.INTERACTION },
                            onNavigateToFace = { currentRoute = AppRoute.FACE_MANAGEMENT },
                            onNavigateToBle = { currentRoute = AppRoute.BLE_MANAGEMENT },
                            onNavigateToRemoteDesktop = { ip ->
                                remoteDesktopIp = ip
                                currentRoute = AppRoute.REMOTE_DESKTOP
                            }
                        )
                    }
                    AppRoute.INTERACTION -> {
                        InteractionScreen(
                            onBackClick = { currentRoute = AppRoute.MAIN_CONTROL },
                            onEditMap = { map ->
                                editingMap = map
                                currentRoute = AppRoute.MAP_EDIT
                            }
                        )
                    }
                    AppRoute.FACE_MANAGEMENT -> {
                        FaceScreen(
                            onBack = { currentRoute = AppRoute.MAIN_CONTROL }
                        )
                    }
                    AppRoute.MAP_EDIT -> {
                        editingMap?.let { map ->
                            MapEditScreen(
                                mapBean = map,
                                onBack = { currentRoute = AppRoute.INTERACTION }
                            )
                        }
                    }
                    AppRoute.BLE_MANAGEMENT -> {
                        BleManagementScreen(onBack = { currentRoute = AppRoute.MAIN_CONTROL })
                    }
                    AppRoute.REMOTE_DESKTOP -> {
                        RemoteDesktopScreen(
                            prefilledIp = remoteDesktopIp,
                            onBack = { currentRoute = AppRoute.MAIN_CONTROL }
                        )
                    }
                }
            }
        }
    }
}
