package com.example.guangnian.feature.console.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.utils.VolumeKeyInterceptor
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.console.ui.panel.*
import com.example.guangnian.feature.console.ui.components.*
import com.example.guangnian.feature.console.viewmodel.*
import com.example.guangnian.feature.console.data.bean.RobotNavState
import com.example.guangnian.feature.main.viewmodel.RobotViewModel

// 动作执行确认弹窗数据载荷
data class ActionConfirmDialogState(
    val title: String = "确认执行",
    val message: String,
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun InteractionScreen(
    onBackClick: () -> Unit = {},
    onEditMap: (com.example.guangnian.feature.console.data.bean.MapBean) -> Unit = {}
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
    var showLogDialog by remember { mutableStateOf(false) }
    var inspectionTab by remember { mutableStateOf("导览点") }
    var onAddClickAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var addActionText by remember { mutableStateOf("") }
    var customHeaderActions by remember { mutableStateOf<@Composable (() -> Unit)?>(null) }

    LaunchedEffect(activeFragment) {
        onAddClickAction = null
        addActionText = ""
        customHeaderActions = null
    }

    // 动作执行确认弹窗状态 (包含标题与安全提示信息)
    var confirmActionDialog by remember { mutableStateOf<ActionConfirmDialogState?>(null) }

    val menus = listOf("姿态动作", "动作", "动作编辑", "导览管理", "语音合成", "智能体设置", "系统设置", "地图管理", "知识库", "日志")

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
        consoleViewModel.fetchAgentStatus()
    }

    LaunchedEffect(activeFragment, inspectionTab) {
        when (activeFragment) {
            "姿态动作" -> consoleViewModel.fetchPostureList()
            "动作" -> consoleViewModel.fetchArmList()
            "地图管理" -> consoleViewModel.fetchMaps()
            "系统设置" -> consoleViewModel.fetchSystemSettings()
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
                backgroundColor = Color.White,
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

        if (confirmActionDialog != null) {
            AlertDialog(
                onDismissRequest = { confirmActionDialog = null },
                title = { Text(confirmActionDialog?.title ?: "确认执行", color = Color.Black, fontWeight = FontWeight.Bold) },
                text = { Text(confirmActionDialog?.message ?: "", color = Color.DarkGray) },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        confirmActionDialog?.onConfirm?.invoke()
                        confirmActionDialog = null
                    }) {
                        Text("确定", color = Color(0xFF007AFF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmActionDialog = null }) {
                        Text("取消", color = Color.Gray)
                    }
                }
            )
        }

        TopStatusBar(
            navStatus = uiState.robotNavStatus,
            systemInfo = uiState.systemInfo,
            connectionState = connectionState,
            connectionError = connectionError,
            latency = latency,
            onBackClick = onBackClick,
            onNavStatusClick = consoleViewModel::fetchNavStatus,
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
                confirmActionDialog = ActionConfirmDialogState(
                    message = "确定要执行 “$actionName” 吗？",
                    onConfirm = {
                        if (uiState.isInspectionPaused) {
                            consoleViewModel.resumeNavigation()
                        } else {
                            consoleViewModel.pauseNavigation()
                        }
                    }
                )
            },
            onCancelClick = {
                confirmActionDialog = ActionConfirmDialogState(
                    message = "确定要执行 “取消导航” 吗？",
                    onConfirm = { consoleViewModel.stopNavigation() }
                )
            },
            onEndMappingClick = {
                if (!uiState.isEndingMapping) {
                    confirmActionDialog = ActionConfirmDialogState(
                        message = "确定要执行 “结束建图” 吗？",
                        onConfirm = { consoleViewModel.endMapping() }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 24.dp)
        )

        AnimatedVisibility(
            visible = true,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 64.dp, start = 24.dp),
            enter = fadeIn(tween(250)) + slideInHorizontally(initialOffsetX = { -40 }),
            exit = fadeOut(tween(200)) + slideOutHorizontally(targetOffsetX = { -40 })
        ) {
            AgentControlPanel(
                status = uiState.agentStatus,
                onWakeUpToggle = consoleViewModel::toggleAgentWakeUp,
                onAsrToggle = consoleViewModel::toggleAgentAsr,
                onInterruptClick = consoleViewModel::interruptDialogue
            )
        }

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
                    confirmActionDialog = ActionConfirmDialogState(
                        title = "确认执行动作",
                        message = "确定执行手臂动作「${fav.name}」（ID: ${fav.id}）吗？\n请确认机器人周围环境安全。",
                        onConfirm = { consoleViewModel.sendArmCommand(fav.id) }
                    )
                } else {
                    confirmActionDialog = ActionConfirmDialogState(
                        title = "确认执行姿态",
                        message = "确定执行姿态「${fav.name}」吗？\n此操作会改变机器人姿态，请确认周围环境安全。",
                        onConfirm = { consoleViewModel.sendPostureCommand(fav.id) }
                    )
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
                    confirmActionDialog = ActionConfirmDialogState(
                        message = "确定要执行 “恢复” 吗？",
                        onConfirm = { consoleViewModel.recover() }
                    )
                } else {
                    confirmActionDialog = ActionConfirmDialogState(
                        message = "确定要执行 “急停” 吗？",
                        onConfirm = { consoleViewModel.stopAll() }
                    )
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
                    if (menu == "日志") {
                        showLogDialog = true
                    } else {
                        activeFragment = if (activeFragment == menu) null else menu
                    }
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
                                confirmActionDialog = ActionConfirmDialogState(
                                    title = "确认执行姿态",
                                    message = "确定执行姿态「$name」吗？\n此操作会改变机器人姿态，请确认周围环境安全。",
                                    onConfirm = { consoleViewModel.sendPostureCommand(id) }
                                )
                            },
                            onActionLongClick = { id, name -> consoleViewModel.toggleFavorite(id, name, false) },
                            onRetryClick = { consoleViewModel.fetchPostureList() }
                        )
                        "动作" -> ActionPanel(
                            armActions = uiState.armActions,
                            favoriteActions = uiState.favoriteActions,
                            onActionClick = { id, name ->
                                confirmActionDialog = ActionConfirmDialogState(
                                    title = "确认执行动作",
                                    message = "确定执行手臂动作「$name」（ID: $id）吗？\n请确认机器人周围环境安全。",
                                    onConfirm = { consoleViewModel.sendArmCommand(id) }
                                )
                            },
                            onActionLongClick = { id, name -> consoleViewModel.toggleFavorite(id, name, true) },
                            onRetryClick = { consoleViewModel.fetchArmList() }
                        )
                        "动作编辑" -> {
                            val onSetActions: (@Composable () -> Unit) -> Unit = remember {
                                { actions -> customHeaderActions = actions }
                            }
                            com.example.guangnian.feature.motion.ui.ActionEditingPanel(onSetActions = onSetActions)
                        }
                        "导览管理" -> InspectionPointPanel(
                            endpoints = uiState.endpoints,
                            routes = uiState.routes,
                            robotPosition = mapUiState.robotPosition,
                            selectedTab = inspectionTab,
                            onPointClick = { id ->
                                val name = uiState.endpoints.find { it.point_id == id }?.name ?: "该点位"
                                confirmActionDialog = ActionConfirmDialogState(
                                    message = "确定前往导览点 “$name” 吗？",
                                    onConfirm = { consoleViewModel.startInspectionPoint(id) }
                                )
                            },
                            onAddCurrentPosition = { consoleViewModel.addInspectionPoint(it) },
                            onDeletePoint = { consoleViewModel.deleteInspectionPoint(it) },
                            onUpdatePoint = { consoleViewModel.updateInspectionPoint(it) },
                            onRouteClick = { id ->
                                val name = uiState.routes.find { it.route_id == id }?.route_name ?: "该路线"
                                confirmActionDialog = ActionConfirmDialogState(
                                    message = "确定开始路线 “$name” 吗？",
                                    onConfirm = { consoleViewModel.startRoute(id) }
                                )
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
                        "系统设置" -> SystemSettingsPanel()
                        "知识库" -> KnowledgeBasePanel(onSetAddAction = { text, action ->
                            addActionText = text
                            onAddClickAction = action
                        })
                        "地图管理" -> MapManagementPanel(
                            maps = uiState.maps,
                            isSwitchingMap = uiState.isSwitchingMap,
                            onDeleteMap = { mapUid ->
                                consoleViewModel.deleteMap(mapUid)
                            },
                            onSwitchMap = { mapUid ->
                                consoleViewModel.switchMap(mapUid)
                            },
                            onEditMap = onEditMap,
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
            levelMode = JoystickLevelMode.Horizontal,
            onMove = { x, _, isDragging ->
                rightJoyX = x
                isRightJoyActive = isDragging
                consoleViewModel.updateJoystickInput(leftJoyX, leftJoyY, rightJoyX, isLeftJoyActive || isRightJoyActive)
            }
        )

        if (showLogDialog) {
            LogDialog(onClose = { showLogDialog = false })
        }

        if (uiState.isEndingMapping) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEE1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF007AFF),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "正在结束建图...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
