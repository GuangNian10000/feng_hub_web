package com.example.guangnian.feature.robot.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guangnian.feature.robot.screen.panel.*
import com.example.guangnian.feature.robot.viewmodel.ActionViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InteractionScreen(
    onBackClick: () -> Unit = {}
) {
    // 绑定 ViewModel 并订阅聚合状态流
    val actionViewModel: ActionViewModel = viewModel { ActionViewModel() }
    val uiState by actionViewModel.uiState.collectAsState()

    // 界面控制状态
    var isMenuOpen by remember { mutableStateOf(false) }
    var isPosturesExpanded by remember { mutableStateOf(true) }
    var activeFragment by remember { mutableStateOf<String?>(null) }
    var isInspectionPaused by remember { mutableStateOf(false) }

    val menus = listOf("动作", "语音合成", "智能体设置", "地图录制", "知识库")

    // 监控面板状态，激活动作面板且数据为空时自动请求列表
    LaunchedEffect(activeFragment) {
        if (activeFragment == "动作" && uiState.postures.isEmpty()) {
            actionViewModel.fetchPostureList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // 1. 视觉背景渲染层
        FullScreenBackgroundStream(activeFragment)

        // 2. 顶部系统状态栏
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

            GlassBox(modifier = Modifier.height(36.dp)) {
                Row(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CONNECTED", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("197 KB/s", color = Color.LightGray, fontSize = 10.sp)
                }
            }
        }

        // 3. 任务控制台
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

        // 4. 底部动态姿态快捷栏
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
                    // 功能菜单入口
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable {
                                isMenuOpen = !isMenuOpen
                                // 展开菜单时，清除当前业务片段状态，关闭中央弹窗
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

                    // 姿态列表平滑展开容器
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
                                if (uiState.postures.isEmpty()) {
                                    // 无数据时的手动重试触发器
                                    Text(
                                        text = if (uiState.isLoading) "加载中..." else "获取失败，点击重试",
                                        color = Color.LightGray,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { actionViewModel.fetchPostureList() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                } else {
                                    // 遍历下发动态动作指令
                                    uiState.postures.forEach { posture ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { actionViewModel.sendPostureCommand(posture.posture_name) }
                                                .padding(horizontal = 10.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = posture.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

        // 5. 急停响应
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

        // 6. 模块选择气泡菜单
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

        // 7. 业务子面板层
        AnimatedVisibility(
            visible = activeFragment != null,
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
                            "动作" -> ActionPanel(
                                postures = uiState.postures,
                                onActionClick = { actionViewModel.sendPostureCommand(it) },
                                onRetryClick = { actionViewModel.fetchPostureList() }
                            )
                            "语音合成" -> SpeechSynthesisPanel()
                            "智能体设置" -> AgentSettingsPanel()
                            "地图录制" -> MapPanel()
                            "知识库" -> KnowledgeBasePanel()
                        }
                    }
                }
            }
        }

        // 8. 双摇杆动力控制
        ComposeJoystick(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 24.dp).size(130.dp),
            onMove = { x, y -> /* 转换线性运动坐标 */ }
        )
        ComposeJoystick(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 24.dp).size(130.dp),
            onMove = { x, y -> /* 转换旋转运动坐标 */ }
        )
    }
}

// ---------------- 辅助 UI 组件 ----------------

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
fun FullScreenBackgroundStream(activeFragment: String?) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 50.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (i in 0..(size.width / gridSize).toInt()) {
            drawLine(color = Color(0x22FFFFFF), start = Offset(i * gridSize, 0f), end = Offset(i * gridSize, size.height), strokeWidth = 1f, pathEffect = pathEffect)
        }
        for (i in 0..(size.height / gridSize).toInt()) {
            drawLine(color = Color(0x22FFFFFF), start = Offset(0f, i * gridSize), end = Offset(size.width, i * gridSize), strokeWidth = 1f, pathEffect = pathEffect)
        }
    }
    if (activeFragment == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("V I S I O N  O N", color = Color(0x55FFFFFF), fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text("感知系统同步中", color = Color(0x55FFFFFF), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ComposeJoystick(modifier: Modifier = Modifier, onMove: (x: Float, y: Float) -> Unit) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }
    Box(
        modifier = modifier
            .background(Color(0x33000000), CircleShape)
            .border(1.dp, Color(0x22FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { thumbOffset = Offset.Zero; onMove(0f, 0f) },
                    onDragCancel = { thumbOffset = Offset.Zero; onMove(0f, 0f) }
                ) { change, dragAmount ->
                    change.consume()
                    val newX = thumbOffset.x + dragAmount.x
                    val newY = thumbOffset.y + dragAmount.y
                    val distance = hypot(newX, newY)
                    thumbOffset = if (distance <= radius) Offset(newX, newY) else {
                        val angle = atan2(newY, newX)
                        Offset(cos(angle) * radius, sin(angle) * radius)
                    }
                    onMove(thumbOffset.x / radius, -thumbOffset.y / radius)
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