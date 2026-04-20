package com.example.guangnian.feature.robot.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.robot.data.bean.MapConfigBean
import com.example.guangnian.feature.robot.data.bean.Point2DBean
import com.example.guangnian.feature.robot.screen.panel.*
import com.example.guangnian.feature.robot.viewmodel.ActionViewModel
import com.example.guangnian.feature.robot.viewmodel.MapViewModel
import com.example.guangnian.feature.robot.viewmodel.MapUiState
import com.example.guangnian.feature.robot.viewmodel.RobotViewModel
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

    var isMenuOpen by remember { mutableStateOf(false) }
    var isPosturesExpanded by remember { mutableStateOf(true) }
    var activeFragment by remember { mutableStateOf<String?>(null) }
    var isInspectionPaused by remember { mutableStateOf(false) }

    val menus = listOf("姿态动作", "动作", "语音合成", "智能体设置", "地图录制", "知识库")

    var leftJoyX by remember { mutableFloatStateOf(0f) }
    var leftJoyY by remember { mutableFloatStateOf(0f) }
    var isLeftJoyActive by remember { mutableStateOf(false) }

    var rightJoyX by remember { mutableFloatStateOf(0f) }
    var isRightJoyActive by remember { mutableStateOf(false) }

    // 控制菜单切换时自动加载数据
    LaunchedEffect(activeFragment) {
        if (activeFragment == "姿态动作" && uiState.postures.isEmpty()) {
            actionViewModel.fetchPostureList()
        }
        if (activeFragment == "动作" && uiState.armActions.isEmpty()) {
            actionViewModel.fetchArmList()
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("》 ${activeFragment ?: ""}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "关闭",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { activeFragment = null }.padding(4.dp)
                        )
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
                            "语音合成" -> SpeechSynthesisPanel()
                            "智能体设置" -> AgentSettingsPanel()
                            "知识库" -> KnowledgeBasePanel()
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
// 辅助渲染组件与工具方法（统一放在该文件下解决引用丢失问题）
// ==============================================================================

@Composable
fun MapBackgroundLayer(
    uiState: MapUiState,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 10f)
                    offset += pan
                }
            }
    ) {
        val config = uiState.mapConfig
        val mapBitmap = uiState.mapImage

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            if (config != null) {
                val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f

                // 1. 绘制栅格地图底图
                if (mapBitmap != null) {
                    drawImage(image = mapBitmap, topLeft = Offset.Zero)
                }

                // 2. 绘制路径
                drawPathLine(uiState.globalPath, config, mapHeightPx, Color(0xFF03DC6B), 3f)
                drawPathLine(uiState.localPath, config, mapHeightPx, Color(0xFFF3D65A), 4f)

                // 3. 绘制点云
                if (uiState.robotPosition != null && uiState.pointCloud.isNotEmpty()) {
                    val worldCloud = mapViewModel.calculatePointCloudWorldCoords(uiState.pointCloud, uiState.robotPosition)
                    worldCloud.forEach { p ->
                        val px = worldToPixel(p.x, p.y, config, mapHeightPx)
                        drawCircle(color = Color.Red.copy(alpha = 0.8f), radius = 2f / scale, center = px)
                    }
                }

                // 4. 绘制导览点
                uiState.endpoints.forEach { ep ->
                    val px = worldToPixel(ep.x, ep.y, config, mapHeightPx)
                    drawCircle(color = Color(0xFF9C27B0), radius = 8f / scale, center = px)
                    drawDirectionArrow(px, 12f / scale, ep.yaw, Color(0xFF9C27B0))
                }

                // 5. 绘制机器狗本体
                uiState.robotPosition?.let { robot ->
                    val px = worldToPixel(robot.x, robot.y, config, mapHeightPx)
                    drawCircle(color = Color(0xFFFFA500), radius = 10f / scale, center = px)
                    drawDirectionArrow(px, 14f / scale, robot.yaw, Color.Red)
                }
            } else {
                // 地图未加载时的备用网格线
                drawFallbackGrid()
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
    val px = (worldX - config.origin[0]) / config.resolution
    val py = mapHeightPx - (worldY - config.origin[1]) / config.resolution
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

private fun DrawScope.drawDirectionArrow(center: Offset, length: Float, yaw: Double, color: Color) {
    val canvasYaw = -yaw // Canvas Y轴向下，需取反
    val tipX = center.x + length * cos(canvasYaw).toFloat()
    val tipY = center.y + length * sin(canvasYaw).toFloat()

    val baseWidth = length * 0.6f
    val baseAngle1 = canvasYaw + PI / 2
    val baseAngle2 = canvasYaw - PI / 2

    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(center.x + baseWidth * cos(baseAngle1).toFloat(), center.y + baseWidth * sin(baseAngle1).toFloat())
        lineTo(center.x + baseWidth * cos(baseAngle2).toFloat(), center.y + baseWidth * sin(baseAngle2).toFloat())
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