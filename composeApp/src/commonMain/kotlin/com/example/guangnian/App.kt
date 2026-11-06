package com.example.guangnian

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.core.network.tcp.KmpTcpClient
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.di.appModule
import com.example.guangnian.feature.console.data.event.RobotEvent
import com.example.guangnian.feature.console.data.event.RobotEventBus
import com.example.guangnian.feature.update.AppUpdater
import com.example.guangnian.feature.blue.BleManagementScreen
import com.example.guangnian.feature.console.ui.InteractionScreen
import com.example.guangnian.feature.main.ui.MainControlScreen
import com.example.guangnian.feature.remote.RemoteDesktopScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

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
        val eventBus = koinInject<RobotEventBus>()
        var globalErrorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(eventBus) {
            eventBus.events.collect { event ->
                if (event is RobotEvent.Error) {
                    // TCP 未连接时，不弹窗提示
                    if (KmpTcpClient.connectionState.value == TcpState.CONNECTED) {
                        globalErrorMessage = event.message
                    }
                }
            }
        }

        // 当 TCP 断开连接时，自动清除现存的错误弹窗
        LaunchedEffect(Unit) {
            KmpTcpClient.connectionState.collect { state ->
                if (state != TcpState.CONNECTED) {
                    globalErrorMessage = null
                }
            }
        }

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
                            onNavigateToBle = { currentRoute = AppRoute.BLE_MANAGEMENT }
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
                        BleManagementScreen(
                            onBack = { currentRoute = AppRoute.MAIN_CONTROL },
                            onNavigateToRemoteDesktop = { ip ->
                                remoteDesktopIp = ip
                                currentRoute = AppRoute.REMOTE_DESKTOP
                            }
                        )
                    }
                    AppRoute.REMOTE_DESKTOP -> {
                        RemoteDesktopScreen(
                            prefilledIp = remoteDesktopIp,
                            onBack = { currentRoute = AppRoute.MAIN_CONTROL }
                        )
                    }
                }
            }

            if (globalErrorMessage != null) {
                AlertDialog(
                    onDismissRequest = { globalErrorMessage = null },
                    title = {
                        Text(
                            text = "提示",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = globalErrorMessage!!,
                            color = Color(0xFF1F2937),
                            fontSize = 14.sp
                        )
                    },
                    backgroundColor = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    confirmButton = {
                        TextButton(onClick = { globalErrorMessage = null }) {
                            Text(
                                text = "确定",
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                )
            }
        }
    }
}
