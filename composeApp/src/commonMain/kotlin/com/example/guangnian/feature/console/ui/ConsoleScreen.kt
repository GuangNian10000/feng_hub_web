package com.example.guangnian.feature.console.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.console.data.bean.MapConfigBean
import com.example.guangnian.feature.console.data.bean.Point2DBean
import com.example.guangnian.feature.console.ui.panel.*
import com.example.guangnian.feature.console.viewmodel.*
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    // 引入地图 ViewModel
    val mapViewModel: MapViewModel = viewModel { MapViewModel() }
    val mapUiState by mapViewModel.uiState.collectAsState()
    val pendingPoint by mapViewModel.pendingPoint.collectAsState()

    var isMenuOpen by remember { mutableStateOf(false) }
    var isPosturesExpanded by remember { mutableStateOf(true) }
    var activeFragment by remember { mutableStateOf<String?>(null) }
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

    // 控制菜单切换时自动加载数据
    LaunchedEffect(activeFragment) {
        when (activeFragment) {
            "姿态动作" -> actionViewModel.fetchPostureList()
            "动作" -> actionViewModel.fetchArmList()
            "导览管理" -> actionViewModel.fetchInspectionPoints()
        }
    }

    // 当从地图交互产生待处理点位时，确保导览管理面板已打开，以便触发编辑弹窗
    LaunchedEffect(pendingPoint) {
        if (pendingPoint != null && activeFragment != "导览管理") {
            KLog.d("ConsoleScreen", "检测到待处理点位，自动切换到导览管理面板")
            activeFragment = "导览管理"
        }
    }

    // 摇杆混合发送逻辑
    LaunchedEffect(isLeftJoyActive, isRightJoyActive) {
        if (isLeftJoyActive || isRightJoyActive) {
            while (isActive) {
                val distance = hypot(leftJoyX, leftJoyY)
                val translateLevel = (distance * 10).toInt().coerceIn(0, 10)
                var angle = 0.0
                if (translateLevel > 0) {
                    angle = atan2(-leftJoyX.toDouble(), leftJoyY.toDouble()) * 180 / PI
                    if (angle < 0) angle += 360
                }

                val rotateLevel = (-rightJoyX * 10).toInt().coerceIn(-10, 10)

                actionViewModel.moveMixed(angle, translateLevel, rotateLevel)
                delay(100)
            }
        } else {
            actionViewModel.stopTranslate()
            actionViewModel.stopRotate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // --- 1. 最底层：交互式 SLAM 地图 ---
        MapBackgroundLayer(
            uiState = mapUiState,
            mapViewModel = mapViewModel,
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. 悬浮 UI 控件层 ---
        // 顶部状态栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
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
                    Divider(modifier = Modifier.fillMaxHeight(0.5f).width(1.dp), color = Color.Gray)
                    Text("BAT 85%", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("TEMP 42°C", color = Color.White, fontSize = 12.sp)
                }
            }

            GlassBox(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (connectionState == TcpState.DISCONNECTED) {
                            robotViewModel.connect()
                        }
                    }
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

        // 右上角巡检控制
        GlassBox(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 64.dp, end = 24.dp)
                .width(120.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(6.dp)
            ) {
                val pauseBtnText = if (isInspectionPaused) "继续巡检" else "暂停巡检"
                val pauseBtnColor = if (isInspectionPaused) Color(0xFF388E3C) else Color(0xFFC2185B)

                ControlButton(
                    text = pauseBtnText,
                    bgColor = pauseBtnColor,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    onClick = { isInspectionPaused = !isInspectionPaused }
                )

                ControlButton(
                    text = "取消巡检",
                    bgColor = Color(0xFF616161),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }
        }

        // 底部动作收藏栏
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 180.dp, end = 252.dp, bottom = 24.dp)
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
                            .clickable {
                                isMenuOpen = !isMenuOpen
                                if (isMenuOpen) {
                                    activeFragment = null
                                }
                            },
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
                                if (uiState.favoriteActions.isEmpty()) {
                                    Text(
                                        text = "暂无收藏动作，请在菜单中长按添加",
                                        color = Color.LightGray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                } else {
                                    uiState.favoriteActions.forEach { fav ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    if (fav.isArmAction) {
                                                        actionViewModel.sendArmCommand(fav.id.toInt())
                                                    } else {
                                                        actionViewModel.sendPostureCommand(fav.id)
                                                    }
                                                }
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
                            .clickable { isPosturesExpanded = !isPosturesExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = isPosturesExpanded, label = "ExpandIconAnim") { expanded ->
                            Text(text = if (expanded) "❯" else "❮", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 急停按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 180.dp, bottom = 24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935).copy(alpha = 0.95f))
                .border(2.dp, Color(0x44FFFFFF), CircleShape)
                .clickable { actionViewModel.stopAll() },
            contentAlignment = Alignment.Center
        ) {
            Text("急停", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }

        // 弹出菜单
        AnimatedVisibility(
            visible = isMenuOpen,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 180.dp, bottom = 88.dp),
            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { 40 }) + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0f, 1f)) + slideOutVertically(targetOffsetY = { 20 })
        ) {
            GlassBox(modifier = Modifier.width(130.dp).heightIn(max = 240.dp), bgColor = Color(0xAA000000)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(4.dp)) {
                    items(menus) { menu ->
                        MenuListItem(
                            text = menu,
                            isSelected = activeFragment == menu,
                            onClick = {
                                activeFragment = if (activeFragment == menu) null else menu
                                isMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // 弹出的控制面板
        AnimatedVisibility(
            visible = activeFragment != null && activeFragment != "地图录制", // 避免遮挡地图层
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, bottom = 96.dp, start = 180.dp, end = 180.dp),
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { -60 }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -40 })
        ) {
            GlassBox(modifier = Modifier.fillMaxWidth().fillMaxHeight(), bgColor = Color(0xD9121212)) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "《 ${activeFragment ?: ""}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { activeFragment = null }
                                .padding(4.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        if (onAddClickAction != null) {
                            Button(
                                onClick = { onAddClickAction?.invoke() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp).padding(end = 12.dp)
                            ) {
                                Text(addActionText, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeFragment) {
                            "姿态动作" -> ActionPosturePanel(
                                postures = uiState.postures,
                                favoriteActions = uiState.favoriteActions,
                                onActionClick = { actionViewModel.sendPostureCommand(it) },
                                onActionLongClick = { id, name ->
                                    actionViewModel.toggleFavorite(id, name, false)
                                },
                                onRetryClick = { actionViewModel.fetchPostureList() }
                            )
                            "动作" -> ActionPanel(
                                armActions = uiState.armActions,
                                favoriteActions = uiState.favoriteActions,
                                onActionClick = { actionViewModel.sendArmCommand(it) },
                                onActionLongClick = { id, name ->
                                    actionViewModel.toggleFavorite(id.toString(), name, true)
                                },
                                onRetryClick = { actionViewModel.fetchArmList() }
                            )
                            "导览管理" -> InspectionPointPanel(
                                endpoints = uiState.endpoints,
                                robotPosition = mapUiState.robotPosition,
                                onPointClick = { actionViewModel.startInspectionPoint(it) },
                                onAddCurrentPosition = { actionViewModel.addInspectionPoint(it) },
                                onDeletePoint = { actionViewModel.deleteInspectionPoint(it) },
                                onUpdatePoint = { actionViewModel.updateInspectionPoint(it) },
                                onRetryClick = { actionViewModel.fetchInspectionPoints() },
                                onSetAddAction = { text, action ->
                                    addActionText = text
                                    onAddClickAction = action
                                },
                                pendingPoint = pendingPoint,
                                onClearPendingPoint = { mapViewModel.clearPendingPoint() }
                            )
                            "语音合成" -> SpeechSynthesisPanel(
                                onSetAddAction = { text, action ->
                                    addActionText = text
                                    onAddClickAction = action
                                }
                            )
                            "智能体设置" -> AgentSettingsPanel(
                                onSetAddAction = { text, action ->
                                    addActionText = text
                                    onAddClickAction = action
                                }
                            )
                            "知识库" -> KnowledgeBasePanel(
                                onSetAddAction = { text, action ->
                                    addActionText = text
                                    onAddClickAction = action
                                }
                            )
                        }
                    }
                }
            }
        }

        // 左右双摇杆
        ComposeJoystick(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 24.dp).size(130.dp),
            onMove = { x, y, isDragging ->
                leftJoyX = x
                leftJoyY = y
                isLeftJoyActive = isDragging
            }
        )
        ComposeJoystick(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 24.dp).size(130.dp),
            onMove = { x, y, isDragging ->
                rightJoyX = x
                isRightJoyActive = isDragging
            }
        )
    }
}

// ==============================================================================
// 辅助渲染组件与工具方法
// ==============================================================================

@Composable
fun MapBackgroundLayer(
    uiState: MapUiState,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val config = uiState.mapConfig
    var scale by remember(config) { mutableFloatStateOf(1f) }
    var offset by remember(config) { mutableStateOf(Offset.Zero) }
    var isInitialized by remember(config) { mutableStateOf(false) }

    val mapBitmap = uiState.mapImage

    fun pixelToWorld(pixel: Offset, config: MapConfigBean, mapHeightPx: Float): Point2DBean {
        val res = config.resolution
        val originX = if (config.origin.size >= 1) config.origin[0] else 0.0
        val originY = if (config.origin.size >= 2) config.origin[1] else 0.0
        val worldX = pixel.x * res + originX
        val worldY = (mapHeightPx - pixel.y) * res + originY
        return Point2DBean(worldX, worldY)
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .pointerInput(config) {
                    if (config == null) return@pointerInput
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.1f, 10f)
                        offset = centroid + pan - (centroid - offset) * (newScale / oldScale)
                        scale = newScale
                    }
                }
                .pointerInput(config, uiState.interactionMode, scale, offset, mapBitmap) {
                    if (config == null) return@pointerInput
                    detectTapGestures(
                        onLongPress = { screenOffset ->
                            val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f
                            val mapPixelOffset = (screenOffset - offset) / scale
                            val worldPos = pixelToWorld(mapPixelOffset, config, mapHeightPx)
                            KLog.d("MapDebug", "长按创建点位: 屏幕=$screenOffset, 像素=$mapPixelOffset, 世界=$worldPos")
                            mapViewModel.onMapLongClick(screenOffset, worldPos)
                        },
                        onTap = { screenOffset ->
                            when (uiState.interactionMode) {
                                MapInteractionMode.PICK_ORIENTATION -> {
                                    val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f
                                    val mapPixelOffset = (screenOffset - offset) / scale
                                    val worldPos = pixelToWorld(mapPixelOffset, config, mapHeightPx)
                                    mapViewModel.onMapClickForOrientation(screenOffset, worldPos)
                                }
                                MapInteractionMode.CONFIRM_POINT -> {
                                    // 处于确认模式时，点击地图空白处不自动关闭，防止误触
                                }
                                else -> {
                                    mapViewModel.clearInteraction()
                                }
                            }
                        }
                    )
                }
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()

            LaunchedEffect(mapBitmap, containerWidth, containerHeight) {
                if (mapBitmap != null && !isInitialized && containerWidth > 0 && containerHeight > 0) {
                    val bmpW = mapBitmap.width.toFloat()
                    val bmpH = mapBitmap.height.toFloat()
                    val scaleW = containerWidth / bmpW
                    val scaleH = containerHeight / bmpH
                    val targetScale = (if (scaleW < scaleH) scaleW else scaleH) * 0.9f
                    val finalScale = if (targetScale < 1.0f) targetScale else 1.0f
                    scale = finalScale
                    offset = Offset((containerWidth - bmpW * finalScale) / 2f, (containerHeight - bmpH * finalScale) / 2f)
                    isInitialized = true
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                if (config != null) {
                    val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f
                    if (mapBitmap != null) {
                        drawImage(image = mapBitmap, topLeft = Offset.Zero)
                    }

                    // 1. 绘制路径
                    drawPathLine(uiState.globalPath, config, mapHeightPx, Color(0xFF03DC6B), 5f / scale)
                    drawPathLine(uiState.localPath, config, mapHeightPx, Color(0xFFF3D65A), 6f / scale)

                    // 2. 绘制导览点
                    uiState.endpoints.forEach { ep ->
                        val realX = ep.position?.x ?: ep.x
                        val realY = ep.position?.y ?: ep.y
                        val realYaw = ep.position?.yaw ?: ep.yaw
                        
                        val px = worldToPixel(realX, realY, config, mapHeightPx)
                        
                        // 导览点：深紫色，半径增大并添加外圈以增强可见度
                        val pointRadius = 12f / scale
                        drawCircle(color = Color(0xFF9C27B0), radius = pointRadius, center = px)
                        drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 15f / scale, center = px, style = Stroke(width = 2f / scale))
                        
                        // 绘制方向箭头，参考老项目增加位移偏移
                        drawDirectionArrow(px, 18f / scale, realYaw, Color(0xFF9C27B0), offset = pointRadius * 1.2f)
                        
                        // 绘制名称
                        // TODO: 使用 nativeCanvas 绘制文本，Compose Canvas 直接绘制文本比较受限
                    }

                    // 3. 绘制临时点
                    if ((uiState.interactionMode == MapInteractionMode.PICK_ORIENTATION || uiState.interactionMode == MapInteractionMode.CONFIRM_POINT) && uiState.tempPoint != null) {
                        val px = worldToPixel(uiState.tempPoint.x, uiState.tempPoint.y, config, mapHeightPx)
                        drawCircle(color = Color(0xFF007AFF), radius = 20f / scale, center = px)
                        drawDirectionArrow(px, 30f / scale, uiState.tempYaw.toDouble(), Color(0xFF007AFF), offset = 22f / scale)
                    }

                    // 4. 绘制机器人位置
                    uiState.robotPosition?.let { robot ->
                        val px = worldToPixel(robot.x, robot.y, config, mapHeightPx)
                        val robotRadius = 18f / scale
                        drawCircle(color = Color(0xFFFFA500), radius = robotRadius, center = px)
                        drawDirectionArrow(px, 25f / scale, robot.yaw, Color.Red, offset = robotRadius * 0.5f)
                    }

                    // 5. 绘制点云 (置于最顶层)
                    if (uiState.robotPosition != null && uiState.pointCloud.isNotEmpty()) {
                        val worldCloud = mapViewModel.calculatePointCloudWorldCoords(uiState.pointCloud, uiState.robotPosition)
                        worldCloud.forEach { p ->
                            val px = worldToPixel(p.x, p.y, config, mapHeightPx)
                            drawCircle(color = Color.Red.copy(alpha = 0.8f), radius = 3f / scale, center = px)
                        }
                    }
                } else {
                    drawFallbackGrid()
                }
            }
        }

        // 交互菜单
        if (uiState.interactionMode == MapInteractionMode.SHOW_MENU && uiState.menuPosition != null) {
            Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(uiState.menuPosition.x.toInt(), uiState.menuPosition.y.toInt()),
                onDismissRequest = { mapViewModel.clearInteraction() }
            ) {
                GlassBox(modifier = Modifier.width(120.dp), bgColor = Color(0xEE2C2C2C)) {
                    Column {
                        Text(
                            "创建导览点",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onSelectCreatePoint() }.padding(12.dp)
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Text(
                            "去此处",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }.padding(12.dp)
                        )
                    }
                }
            }
        }
        
        // 确认点位小窗
        if (uiState.interactionMode == MapInteractionMode.CONFIRM_POINT && uiState.tempPoint != null && config != null) {
            val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f
            // 计算点位在 Canvas 内部的像素坐标
            val px = worldToPixel(uiState.tempPoint.x, uiState.tempPoint.y, config, mapHeightPx)
            // 转换为相对于容器的绝对像素坐标
            val screenX = px.x * scale + offset.x
            val screenY = px.y * scale + offset.y
            
            val density = androidx.compose.ui.platform.LocalDensity.current
            val popupWidthPx = with(density) { 140.dp.toPx() }

            Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(
                    (screenX - popupWidthPx / 2).toInt(),
                    (screenY + 30).toInt() // 避开中心点，稍微向下偏移
                ),
                onDismissRequest = { mapViewModel.clearInteraction() }
            ) {
                GlassBox(modifier = Modifier.width(140.dp), bgColor = Color(0xEE2C2C2C)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "取消",
                            color = Color(0xFFFF5252),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { mapViewModel.clearInteraction() }.padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                        Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.2f))
                        Text(
                            "确认",
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { mapViewModel.onConfirmCreatePoint() }.padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
        
        if (uiState.interactionMode == MapInteractionMode.PICK_ORIENTATION) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xAA000000)).padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("请在地图上点击一个位置以确定方向", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

private fun DrawScope.drawFallbackGrid() {
    val gridSize = 50f
    for (i in 0..(size.width / gridSize).toInt()) {
        drawLine(Color(0x22FFFFFF), Offset(i * gridSize, 0f), Offset(i * gridSize, size.height))
    }
    for (i in 0..(size.height / gridSize).toInt()) {
        drawLine(Color(0x22FFFFFF), Offset(0f, i * gridSize), Offset(size.width, i * gridSize))
    }
}

private fun worldToPixel(worldX: Double, worldY: Double, config: MapConfigBean, mapHeightPx: Float): Offset {
    val originX = if (config.origin.size >= 1) config.origin[0] else 0.0
    val originY = if (config.origin.size >= 2) config.origin[1] else 0.0
    val res = if (config.resolution > 0) config.resolution else 0.05
    
    // px = (真实世界坐标 - 原点坐标) / 分辨率
    val px = (worldX - originX) / res
    // py = 总高度 - (真实世界坐标 - 原点坐标) / 分辨率 (因为 Canvas Y轴向下)
    val py = mapHeightPx - (worldY - originY) / res

    return Offset(px.toFloat(), py.toFloat())
}

private fun DrawScope.drawPathLine(pathData: List<Point2DBean>, config: MapConfigBean, mapHeightPx: Float, color: Color, strokeWidth: Float) {
    if (pathData.size < 2) return
    val path = Path()
    pathData.forEachIndexed { index, point ->
        val px = worldToPixel(point.x, point.y, config, mapHeightPx)
        if (index == 0) path.moveTo(px.x, px.y) else path.lineTo(px.x, px.y)
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawDirectionArrow(center: Offset, length: Float, yaw: Double, color: Color, offset: Float = 0f) {
    val canvasYaw = -yaw // Canvas Y轴向下，需取反
    
    // 计算箭头起点（根据 offset 偏移）
    val startX = center.x + offset * cos(canvasYaw).toFloat()
    val startY = center.y + offset * sin(canvasYaw).toFloat()
    
    // 计算箭头顶点
    val tipX = startX + length * cos(canvasYaw).toFloat()
    val tipY = startY + length * sin(canvasYaw).toFloat()
    
    val baseWidth = length * 0.6f
    val baseAngle1 = canvasYaw + PI / 2
    val baseAngle2 = canvasYaw - PI / 2
    
    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(startX + baseWidth * cos(baseAngle1).toFloat(), startY + baseWidth * sin(baseAngle1).toFloat())
        lineTo(startX + baseWidth * cos(baseAngle2).toFloat(), startY + baseWidth * sin(baseAngle2).toFloat())
        close()
    }
    drawPath(path = path, color = color)
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
