package com.example.guangnian.feature.console.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.utils.VolumeKeyInterceptor
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.console.ui.panel.*
import com.example.guangnian.feature.teach.ui.TeachActionPanel
import com.example.guangnian.feature.console.ui.components.*
import com.example.guangnian.feature.console.viewmodel.*
import com.example.guangnian.feature.console.data.bean.RobotNavState
import com.example.guangnian.feature.main.viewmodel.RobotViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun InteractionScreen(
    onBackClick: () -> Unit = {}
) {
    val consoleViewModel: ConsoleViewModel = koinViewModel()
    val uiState by consoleViewModel.uiState.collectAsState()

    val robotViewModel: RobotViewModel = koinViewModel()
    val connectionState by robotViewModel.connectionState.collectAsState()
    val connectionError by robotViewModel.connectionError.collectAsState()
    val latency by robotViewModel.latency.collectAsState()

    val mapViewModel: MapViewModel = koinViewModel()
    val mapUiState by mapViewModel.uiState.collectAsState()

    val volumeKeyHandler = remember(consoleViewModel) {
        { increase: Boolean -> consoleViewModel.setAgentVolume(increase) }
    }

    DisposableEffect(volumeKeyHandler) {
        VolumeKeyInterceptor.register(volumeKeyHandler)
        onDispose { VolumeKeyInterceptor.unregister(volumeKeyHandler) }
    }

    LifecycleResumeEffect(Unit) {
        consoleViewModel.fetchAgentStatus()
        onPauseOrDispose { }
    }

    val pendingPoint = uiState.pendingPoint
    val deleteConfirmPoint = uiState.deleteConfirmPoint

    var isMenuOpen by remember { mutableStateOf(false) }
    var isPosturesExpanded by remember { mutableStateOf(true) }
    var activeFragment by remember { mutableStateOf<String?>(null) }
    var inspectionTab by remember { mutableStateOf("导览点") }
    var onAddClickAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var addActionText by remember { mutableStateOf("") }
    var customHeaderActions by remember { mutableStateOf<@Composable (() -> Unit)?>(null) }

    LaunchedEffect(activeFragment) {
        onAddClickAction = null
        addActionText = ""
        customHeaderActions = null
    }

    // 动作执行确认弹窗状态
    var confirmActionPayload by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val menus = listOf("姿态动作", "动作", "示教动作", "导览管理", "语音合成", "智能体设置", "地图管理", "知识库")

    var leftJoyX by remember { mutableFloatStateOf(0f) }
    var leftJoyY by remember { mutableFloatStateOf(0f) }
    var isLeftJoyActive by remember { mutableStateOf(false) }

    var rightJoyX by remember { mutableFloatStateOf(0f) }
    var isRightJoyActive by remember { mutableStateOf(false) }

    // 联动优化：当 ActionViewModel 获取到最新点位时，直接同步给 MapViewModel 的 UI 状态
    // 这样添加/删除点位后，地图会瞬间响应，无需等待下一次网络轮询
    LaunchedEffect(uiState.endpoints) {
        mapViewModel.updateEndpoints(uiState.endpoints)
    }

    LaunchedEffect(Unit) {
        consoleViewModel.fetchNavStatus()
    }

    LaunchedEffect(activeFragment, inspectionTab) {
        when (activeFragment) {
            "姿态动作" -> consoleViewModel.fetchPostureList()
            "动作" -> consoleViewModel.fetchArmList()
            "地图管理" -> consoleViewModel.fetchMaps()
            "导览管理" -> {
                consoleViewModel.fetchArmList() // 编辑点位需要动作列表进行名称映射
                if (inspectionTab == "导览点") {
                    consoleViewModel.fetchInspectionPoints()
                } else {
                    consoleViewModel.fetchRoutes()
                }
            }
        }
    }

    LaunchedEffect(pendingPoint) {
        if (pendingPoint != null && activeFragment != "导览管理") {
            KLog.d("ConsoleScreen", "检测到待处理点位，自动切换到导览管理面板")
            activeFragment = "导览管理"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        MapBackgroundLayer(
            uiState = mapUiState,
            mapViewModel = mapViewModel,
            modifier = Modifier.fillMaxSize()
        )

        if (deleteConfirmPoint != null) {
            AlertDialog(
                onDismissRequest = { consoleViewModel.clearDeleteConfirmPoint() },
                title = { Text("确认删除", color = Color.Black, fontWeight = FontWeight.Bold) },
                text = { Text("确定要删除导览点 “${deleteConfirmPoint.name}” 吗？", color = Color.DarkGray) },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        consoleViewModel.deleteInspectionPoint(deleteConfirmPoint.point_id)
                        consoleViewModel.clearDeleteConfirmPoint()
                    }) {
                        Text("删除", color = Color(0xFFFF5252))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { consoleViewModel.clearDeleteConfirmPoint() }) {
                        Text("取消", color = Color.Gray)
                    }
                }
            )
        }

        if (confirmActionPayload != null) {
            AlertDialog(
                onDismissRequest = { confirmActionPayload = null },
                title = { Text("确认执行", color = Color.Black, fontWeight = FontWeight.Bold) },
                text = { Text("确定要执行 “${confirmActionPayload?.first}” 吗？", color = Color.DarkGray) },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        confirmActionPayload?.second?.invoke()
                        confirmActionPayload = null
                    }) {
                        Text("确定", color = Color(0xFF007AFF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmActionPayload = null }) {
                        Text("取消", color = Color.Gray)
                    }
                }
            )
        }

        TopStatusBar(
            navStatus = uiState.robotNavStatus,
            connectionState = connectionState,
            connectionError = connectionError,
            latency = latency,
            onBackClick = onBackClick,
            onConnectClick = {
                if (connectionState == TcpState.DISCONNECTED) {
                    robotViewModel.connect()
                }
            }
        )

        InspectionControlPanel(
            isPaused = uiState.isInspectionPaused,
            showEndMapping = uiState.robotNavStatus.state == RobotNavState.MAPPING,
            onPauseToggle = {
                val actionName = if (uiState.isInspectionPaused) "继续导航" else "暂停导航"
                confirmActionPayload = actionName to {
                    if (uiState.isInspectionPaused) {
                        consoleViewModel.resumeNavigation()
                    } else {
                        consoleViewModel.pauseNavigation()
                    }
                }
            },
            onCancelClick = {
                confirmActionPayload = "取消导航" to {
                    consoleViewModel.stopNavigation()
                }
            },
            onEndMappingClick = {
                confirmActionPayload = "结束建图" to { consoleViewModel.endMapping() }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 24.dp)
        )

        AgentControlPanel(
            status = uiState.agentStatus,
            onWakeUpToggle = consoleViewModel::toggleAgentWakeUp,
            onAsrToggle = consoleViewModel::toggleAgentAsr,
            onInterruptClick = consoleViewModel::interruptDialogue,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 64.dp, start = 24.dp)
        )

        BottomActionBar(
            isMenuOpen = isMenuOpen,
            onMenuToggle = {
                isMenuOpen = !isMenuOpen
                if (isMenuOpen) activeFragment = null
            },
            isPosturesExpanded = isPosturesExpanded,
            onExpandToggle = { isPosturesExpanded = !isPosturesExpanded },
            favoriteActions = uiState.favoriteActions,
            onActionClick = { fav ->
                confirmActionPayload = fav.name to {
                    if (fav.isArmAction) {
                        consoleViewModel.sendArmCommand(fav.id)
                    } else {
                        consoleViewModel.sendPostureCommand(fav.id)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 180.dp, end = 252.dp, bottom = 24.dp)
        )

        EmergencyStopButton(
            isEmergency = uiState.robotNavStatus.state == RobotNavState.EMERGENCY_STOP,
            onClick = {
                if (uiState.robotNavStatus.state == RobotNavState.EMERGENCY_STOP) {
                    confirmActionPayload = "恢复" to { consoleViewModel.recover() }
                } else {
                    confirmActionPayload = "急停" to { consoleViewModel.stopAll() }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 180.dp, bottom = 24.dp)
        )

        AnimatedVisibility(
            visible = isMenuOpen,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 180.dp, end = 180.dp, bottom = 88.dp)
                .fillMaxWidth(),
            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { 40 }) + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin(0.5f, 1f)),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0.5f, 1f)) + slideOutVertically(targetOffsetY = { 20 })
        ) {
            SideMenu(
                menus = menus,
                activeFragment = activeFragment,
                onMenuClick = { menu ->
                    activeFragment = if (activeFragment == menu) null else menu
                    isMenuOpen = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = activeFragment != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, bottom = 96.dp, start = 180.dp, end = 180.dp),
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { -60 }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -40 })
        ) {
            MainControlPanel(
                title = activeFragment ?: "",
                onClose = { activeFragment = null },
                tabs = if (activeFragment == "导览管理") listOf("导览点", "路线") else emptyList(),
                selectedTab = inspectionTab,
                onTabChange = { inspectionTab = it },
                actionButton = customHeaderActions ?: if (onAddClickAction != null) {
                    { PanelAddButton(text = addActionText, onClick = { onAddClickAction?.invoke() }) }
                } else null,
                content = {
                    when (activeFragment) {
                        "姿态动作" -> ActionPosturePanel(
                            postures = uiState.postures,
                            favoriteActions = uiState.favoriteActions,
                            onActionClick = { id, name ->
                                confirmActionPayload = name to { consoleViewModel.sendPostureCommand(id) }
                            },
                            onActionLongClick = { id, name -> consoleViewModel.toggleFavorite(id, name, false) },
                            onRetryClick = { consoleViewModel.fetchPostureList() }
                        )
                        "动作" -> ActionPanel(
                            armActions = uiState.armActions,
                            favoriteActions = uiState.favoriteActions,
                            onActionClick = { id, name ->
                                confirmActionPayload = name to { consoleViewModel.sendArmCommand(id) }
                            },
                            onActionLongClick = { id, name -> consoleViewModel.toggleFavorite(id.toString(), name, true) },
                            onRetryClick = { consoleViewModel.fetchArmList() }
                        )
                        "示教动作" -> {
                            val onSetActions: (@Composable () -> Unit) -> Unit = remember {
                                { actions -> customHeaderActions = actions }
                            }
                            TeachActionPanel(onSetActions = onSetActions)
                        }
                        "导览管理" -> InspectionPointPanel(
                            endpoints = uiState.endpoints,
                            routes = uiState.routes,
                            robotPosition = mapUiState.robotPosition,
                            selectedTab = inspectionTab,
                            onPointClick = { id ->
                                val name = uiState.endpoints.find { it.point_id == id }?.name ?: "该点位"
                                confirmActionPayload = "前往导览点 $name" to { consoleViewModel.startInspectionPoint(id) }
                            },
                            onAddCurrentPosition = { consoleViewModel.addInspectionPoint(it) },
                            onDeletePoint = { consoleViewModel.deleteInspectionPoint(it) },
                            onUpdatePoint = { consoleViewModel.updateInspectionPoint(it) },
                            onRouteClick = { id ->
                                val name = uiState.routes.find { it.route_id == id }?.route_name ?: "该路线"
                                confirmActionPayload = "开始路线 $name" to { consoleViewModel.startRoute(id) }
                            },
                            onAddRoute = { consoleViewModel.addRoute(it) },
                            onDeleteRoute = { consoleViewModel.deleteRoute(it) },
                            onUpdateRoute = { consoleViewModel.updateRoute(it) },
                            onRetryClick = {
                                if (inspectionTab == "导览点") consoleViewModel.fetchInspectionPoints()
                                else consoleViewModel.fetchRoutes()
                            },
                            onSetAddAction = { text, action ->
                                addActionText = text
                                onAddClickAction = action
                            },
                            armActions = uiState.armActions,
                            pendingPoint = pendingPoint,
                            onClearPendingPoint = { consoleViewModel.clearPendingPoint() }
                        )
                        "语音合成" -> SpeechSynthesisPanel(onSetAddAction = { text, action ->
                            addActionText = text
                            onAddClickAction = action
                        })
                        "智能体设置" -> AgentSettingsPanel(onSetAddAction = { text, action ->
                            addActionText = text
                            onAddClickAction = action
                        })
                        "知识库" -> KnowledgeBasePanel(onSetAddAction = { text, action ->
                            addActionText = text
                            onAddClickAction = action
                        })
                        "地图管理" -> MapManagementPanel(
                            maps = uiState.maps,
                            onDeleteMap = { mapUid ->
                                consoleViewModel.deleteMap(mapUid)
                            },
                            onSwitchMap = { mapUid ->
                                consoleViewModel.switchMap(mapUid)
                            },
                            onStartMapping = { name ->
                                consoleViewModel.startMapping(name)
                            },
                            onSetAddAction = { text, action ->
                                addActionText = text
                                onAddClickAction = action
                            },
                            onRetryClick = { consoleViewModel.fetchMaps() }
                        )
                    }
                }
            )
        }

        ComposeJoystick(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 24.dp).size(130.dp),
            onMove = { x, y, isDragging ->
                leftJoyX = x
                leftJoyY = y
                isLeftJoyActive = isDragging
                consoleViewModel.updateJoystickInput(leftJoyX, leftJoyY, rightJoyX, isLeftJoyActive || isRightJoyActive)
            }
        )
        ComposeJoystick(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 24.dp).size(130.dp),
            onMove = { x, _, isDragging ->
                rightJoyX = x
                isRightJoyActive = isDragging
                consoleViewModel.updateJoystickInput(leftJoyX, leftJoyY, rightJoyX, isLeftJoyActive || isRightJoyActive)
            }
        )
    }
}
