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
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.console.ui.panel.*
import com.example.guangnian.feature.console.ui.components.*
import com.example.guangnian.feature.console.viewmodel.*
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

    val mapViewModel: MapViewModel = koinViewModel()
    val mapUiState by mapViewModel.uiState.collectAsState()
    val pendingPoint by mapViewModel.pendingPoint.collectAsState()

    var mapDeleteConfirmPoint by remember { mutableStateOf<com.example.guangnian.feature.console.data.bean.InspectionPointBean?>(null) }
    val deletePointEvent by mapViewModel.deletePointEvent.collectAsState()
    LaunchedEffect(deletePointEvent) {
        deletePointEvent?.let { point ->
            mapDeleteConfirmPoint = point
            mapViewModel.clearDeletePointEvent()
        }
    }

    var isMenuOpen by remember { mutableStateOf(false) }
    var isPosturesExpanded by remember { mutableStateOf(true) }
    var activeFragment by remember { mutableStateOf<String?>(null) }
    var inspectionTab by remember { mutableStateOf("导览点") }
    var onAddClickAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var addActionText by remember { mutableStateOf("") }

    LaunchedEffect(activeFragment) {
        onAddClickAction = null
        addActionText = ""
    }
    var isInspectionPaused by remember { mutableStateOf(false) }

    val menus = listOf("姿态动作", "动作", "导览管理", "语音合成", "智能体设置", "地图录制", "知识库")

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

    LaunchedEffect(activeFragment, inspectionTab) {
        when (activeFragment) {
            "姿态动作" -> consoleViewModel.fetchPostureList()
            "动作" -> consoleViewModel.fetchArmList()
            "导览管理" -> {
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

        if (mapDeleteConfirmPoint != null) {
            AlertDialog(
                onDismissRequest = { mapDeleteConfirmPoint = null },
                title = { Text("确认删除", color = Color.White) },
                text = { Text("确定要删除导览点 “${mapDeleteConfirmPoint?.name}” 吗？", color = Color.LightGray) },
                containerColor = Color(0xFF2C2C2C),
                confirmButton = {
                    TextButton(onClick = {
                        mapDeleteConfirmPoint?.let { consoleViewModel.deleteInspectionPoint(it.pointId) }
                        mapDeleteConfirmPoint = null
                    }) {
                        Text("删除", color = Color(0xFFFF5252))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mapDeleteConfirmPoint = null }) {
                        Text("取消", color = Color.White)
                    }
                }
            )
        }

        TopStatusBar(
            connectionState = connectionState,
            connectionError = connectionError,
            onBackClick = onBackClick,
            onConnectClick = {
                if (connectionState == TcpState.DISCONNECTED) {
                    robotViewModel.connect()
                }
            }
        )

        InspectionControlPanel(
            isPaused = isInspectionPaused,
            onPauseToggle = { isInspectionPaused = !isInspectionPaused },
            onCancelClick = { /* TODO: Implement cancel logic */ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 24.dp)
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
                if (fav.isArmAction) {
                    consoleViewModel.sendArmCommand(fav.id.toInt())
                } else {
                    consoleViewModel.sendPostureCommand(fav.id)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 180.dp, end = 252.dp, bottom = 24.dp)
        )

        EmergencyStopButton(
            onClick = { consoleViewModel.stopAll() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 180.dp, bottom = 24.dp)
        )

        AnimatedVisibility(
            visible = isMenuOpen,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 180.dp, bottom = 88.dp),
            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { 40 }) + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)) + slideOutVertically(targetOffsetY = { 20 })
        ) {
            SideMenu(
                menus = menus,
                activeFragment = activeFragment,
                onMenuClick = { menu ->
                    activeFragment = if (activeFragment == menu) null else menu
                    isMenuOpen = false
                }
            )
        }

        AnimatedVisibility(
            visible = activeFragment != null && activeFragment != "地图录制",
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
                actionButton = if (onAddClickAction != null) {
                    { PanelAddButton(text = addActionText, onClick = { onAddClickAction?.invoke() }) }
                } else null,
                content = {
                    when (activeFragment) {
                        "姿态动作" -> ActionPosturePanel(
                            postures = uiState.postures,
                            favoriteActions = uiState.favoriteActions,
                            onActionClick = { consoleViewModel.sendPostureCommand(it) },
                            onActionLongClick = { id, name -> consoleViewModel.toggleFavorite(id, name, false) },
                            onRetryClick = { consoleViewModel.fetchPostureList() }
                        )
                        "动作" -> ActionPanel(
                            armActions = uiState.armActions,
                            favoriteActions = uiState.favoriteActions,
                            onActionClick = { consoleViewModel.sendArmCommand(it) },
                            onActionLongClick = { id, name -> consoleViewModel.toggleFavorite(id.toString(), name, true) },
                            onRetryClick = { consoleViewModel.fetchArmList() }
                        )
                        "导览管理" -> InspectionPointPanel(
                            endpoints = uiState.endpoints,
                            routes = uiState.routes,
                            robotPosition = mapUiState.robotPosition,
                            selectedTab = inspectionTab,
                            onPointClick = { consoleViewModel.startInspectionPoint(it) },
                            onAddCurrentPosition = { consoleViewModel.addInspectionPoint(it) },
                            onDeletePoint = { consoleViewModel.deleteInspectionPoint(it) },
                            onUpdatePoint = { consoleViewModel.updateInspectionPoint(it) },
                            onRouteClick = { consoleViewModel.startRoute(it) },
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
                            pendingPoint = pendingPoint,
                            onClearPendingPoint = { mapViewModel.clearPendingPoint() }
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
