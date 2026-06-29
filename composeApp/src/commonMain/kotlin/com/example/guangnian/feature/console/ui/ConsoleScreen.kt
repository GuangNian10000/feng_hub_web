package com.example.guangnian.feature.console.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.console.ui.panel.*
import com.example.guangnian.feature.console.viewmodel.*
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import kotlin.math.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun InteractionScreen(
    onBackClick: () -> Unit = {}
) {
    val actionViewModel: ActionViewModel = viewModel { ActionViewModel() }
    val uiState by actionViewModel.uiState.collectAsState()

    val robotViewModel: RobotViewModel = viewModel { RobotViewModel() }
    val connectionState by robotViewModel.connectionState.collectAsState()
    val connectionError by robotViewModel.connectionError.collectAsState()

    val mapViewModel: MapViewModel = viewModel { MapViewModel() }
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

    LaunchedEffect(activeFragment, inspectionTab) {
        when (activeFragment) {
            "姿态动作" -> actionViewModel.fetchPostureList()
            "动作" -> actionViewModel.fetchArmList()
            "导览管理" -> {
                if (inspectionTab == "导览点") {
                    actionViewModel.fetchInspectionPoints()
                } else {
                    actionViewModel.fetchRoutes()
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
                        mapDeleteConfirmPoint?.let { actionViewModel.deleteInspectionPoint(it.pointId) }
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
                    actionViewModel.sendArmCommand(fav.id.toInt())
                } else {
                    actionViewModel.sendPostureCommand(fav.id)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 180.dp, end = 252.dp, bottom = 24.dp)
        )

        EmergencyStopButton(
            onClick = { actionViewModel.stopAll() },
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
                activeFragment = activeFragment,
                inspectionTab = inspectionTab,
                onClose = { activeFragment = null },
                onTabChange = { inspectionTab = it },
                onAddClickAction = onAddClickAction,
                addActionText = addActionText,
                content = {
                    when (activeFragment) {
                        "姿态动作" -> ActionPosturePanel(
                            postures = uiState.postures,
                            favoriteActions = uiState.favoriteActions,
                            onActionClick = { actionViewModel.sendPostureCommand(it) },
                            onActionLongClick = { id, name -> actionViewModel.toggleFavorite(id, name, false) },
                            onRetryClick = { actionViewModel.fetchPostureList() }
                        )
                        "动作" -> ActionPanel(
                            armActions = uiState.armActions,
                            favoriteActions = uiState.favoriteActions,
                            onActionClick = { actionViewModel.sendArmCommand(it) },
                            onActionLongClick = { id, name -> actionViewModel.toggleFavorite(id.toString(), name, true) },
                            onRetryClick = { actionViewModel.fetchArmList() }
                        )
                        "导览管理" -> InspectionPointPanel(
                            endpoints = uiState.endpoints,
                            routes = uiState.routes,
                            robotPosition = mapUiState.robotPosition,
                            selectedTab = inspectionTab,
                            onPointClick = { actionViewModel.startInspectionPoint(it) },
                            onAddCurrentPosition = { actionViewModel.addInspectionPoint(it) },
                            onDeletePoint = { actionViewModel.deleteInspectionPoint(it) },
                            onUpdatePoint = { actionViewModel.updateInspectionPoint(it) },
                            onRouteClick = { actionViewModel.startRoute(it) },
                            onAddRoute = { actionViewModel.addRoute(it) },
                            onDeleteRoute = { actionViewModel.deleteRoute(it) },
                            onUpdateRoute = { actionViewModel.updateRoute(it) },
                            onRetryClick = {
                                if (inspectionTab == "导览点") actionViewModel.fetchInspectionPoints()
                                else actionViewModel.fetchRoutes()
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
                actionViewModel.updateJoystickInput(leftJoyX, leftJoyY, rightJoyX, isLeftJoyActive || isRightJoyActive)
            }
        )
        ComposeJoystick(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 24.dp).size(130.dp),
            onMove = { x, _, isDragging ->
                rightJoyX = x
                isRightJoyActive = isDragging
                actionViewModel.updateJoystickInput(leftJoyX, leftJoyY, rightJoyX, isLeftJoyActive || isRightJoyActive)
            }
        )
    }
}

@Composable
fun TopStatusBar(
    connectionState: TcpState,
    connectionError: String?,
    onBackClick: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        GlassBox(modifier = Modifier.height(36.dp)) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "〈 返回",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onBackClick() }.padding(end = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.fillMaxHeight(0.5f).width(1.dp), color = Color.Gray)
                Text("BAT 85%", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("TEMP 42°C", color = Color.White, fontSize = 12.sp)
            }
        }

        GlassBox(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onConnectClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (connectionState) {
                    TcpState.CONNECTED -> Color(0xFF4CAF50)
                    TcpState.CONNECTING -> Color(0xFFFFC107)
                    TcpState.DISCONNECTED -> Color(0xFFE53935)
                }
                val statusText = when (connectionState) {
                    TcpState.CONNECTED -> "已连接"
                    TcpState.CONNECTING -> "连接中..."
                    TcpState.DISCONNECTED -> "未连接"
                }

                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                if (connectionState == TcpState.DISCONNECTED && !connectionError.isNullOrEmpty()) {
                    Text(
                        text = connectionError ?: "",
                        color = Color(0xFFE53935),
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                } else if (connectionState == TcpState.CONNECTED) {
                    Text("197 KB/s", color = Color.LightGray, fontSize = 10.sp)
                } else if (connectionState == TcpState.CONNECTING) {
                    Text("请稍候", color = Color.LightGray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun InspectionControlPanel(
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier
            .width(120.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(6.dp)
        ) {
            val pauseBtnText = if (isPaused) "继续巡检" else "暂停巡检"
            val pauseBtnColor = if (isPaused) Color(0xFF388E3C) else Color(0xFFC2185B)

            ControlButton(
                text = pauseBtnText,
                bgColor = pauseBtnColor,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                onClick = onPauseToggle
            )

            ControlButton(
                text = "取消巡检",
                bgColor = Color(0xFF616161),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                onClick = onCancelClick
            )
        }
    }
}

@Composable
fun BottomActionBar(
    isMenuOpen: Boolean,
    onMenuToggle: () -> Unit,
    isPosturesExpanded: Boolean,
    onExpandToggle: () -> Unit,
    favoriteActions: List<com.example.guangnian.feature.console.data.bean.FavoriteActionBean>,
    onActionClick: (com.example.guangnian.feature.console.data.bean.FavoriteActionBean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        GlassBox(
            modifier = Modifier
                .wrapContentWidth()
                .height(56.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bgColor by animateColorAsState(
                    targetValue = if (isMenuOpen) Color(0x66007AFF) else Color(0x33FFFFFF),
                    animationSpec = tween(300),
                    label = "MenuBgColor"
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onMenuToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isMenuOpen,
                        transitionSpec = {
                            (scaleIn(tween(200)) + fadeIn(tween(200))).togetherWith(scaleOut(tween(200)) + fadeOut(tween(200)))
                        },
                        label = "MenuIconAnim"
                    ) { open ->
                        Text(
                            text = if (open) "✕" else "⊞",
                            color = Color.White,
                            fontSize = if (open) 16.sp else 18.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f, fill = false),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isPosturesExpanded,
                        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (favoriteActions.isEmpty()) {
                                Text(
                                    text = "暂无收藏动作，请在菜单中长按添加",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            } else {
                                favoriteActions.forEach { fav ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onActionClick(fav) }
                                            .padding(horizontal = 10.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = fav.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onExpandToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = isPosturesExpanded, label = "ExpandIconAnim") { expanded ->
                        Text(text = if (expanded) "❯" else "❮", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyStopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFFE53935).copy(alpha = 0.95f))
            .border(2.dp, Color(0x44FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("急停", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SideMenu(
    menus: List<String>,
    activeFragment: String?,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(modifier = modifier.width(130.dp).heightIn(max = 240.dp), bgColor = Color(0xAA000000)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(4.dp)) {
            items(menus) { menu ->
                MenuListItem(
                    text = menu,
                    isSelected = activeFragment == menu,
                    onClick = { onMenuClick(menu) }
                )
            }
        }
    }
}

@Composable
fun MainControlPanel(
    activeFragment: String?,
    inspectionTab: String,
    onClose: () -> Unit,
    onTabChange: (String) -> Unit,
    onAddClickAction: (() -> Unit)?,
    addActionText: String,
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(modifier = modifier.fillMaxWidth().fillMaxHeight(), bgColor = Color(0xD9121212)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeFragment == "导览管理") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "《 ",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onClose() }
                                .padding(4.dp)
                        )
                        Text(
                            text = "导览点",
                            color = if (inspectionTab == "导览点") Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (inspectionTab == "导览点") FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onTabChange("导览点") }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .then(if (inspectionTab == "导览点") Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "路线",
                            color = if (inspectionTab == "路线") Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (inspectionTab == "路线") FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onTabChange("路线") }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .then(if (inspectionTab == "路线") Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier)
                        )
                    }
                } else {
                    Text(
                        text = "《 ${activeFragment ?: ""}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onClose() }
                            .padding(4.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onAddClickAction != null) {
                    Button(
                        onClick = { onAddClickAction.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp).padding(end = 12.dp)
                    ) {
                        Text(addActionText, fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0x77000000),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .padding(4.dp),
        content = content
    )
}

@Composable
fun SideMenuPopup(
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0x77000000),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .padding(4.dp),
        content = content
    )
}

@Composable
fun MenuListItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0x44FFFFFF) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = if (isSelected) Color.White else Color.LightGray, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ControlButton(text: String, bgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(bgColor.copy(alpha = 0.9f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ComposeJoystick(
    modifier: Modifier = Modifier,
    onMove: (x: Float, y: Float, isDragging: Boolean) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .background(Color(0x33000000), CircleShape)
            .border(1.dp, Color(0x22FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f, false)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f, false)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val newX = thumbOffset.x + dragAmount.x
                    val newY = thumbOffset.y + dragAmount.y
                    val distance = hypot(newX, newY)
                    thumbOffset = if (distance <= radius) Offset(newX, newY) else {
                        val angle = atan2(newY, newX)
                        Offset(cos(angle) * radius, sin(angle) * radius)
                    }
                    onMove(thumbOffset.x / radius, -thumbOffset.y / radius, true)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            radius = size.width / 2f
            drawCircle(color = Color(0xBBFFFFFF), radius = radius * 0.35f, center = center + thumbOffset)
        }
    }
}
