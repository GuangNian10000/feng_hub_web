@file:Suppress("DEPRECATION")

package com.example.guangnian.feature.remote

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


// ─── 设计令牌（与 BLE UI 风格统一）────────────────────────────────────────────

private val RdBg      = Color(0xFF0A0F1E)
private val RdSurface = Color(0xFF131B2E)
private val RdCard    = Color(0xFF1C2840)
private val RdAccent  = Color(0xFF40C4FF)
private val RdError   = Color(0xFFFF5252)
private val RdText    = Color.White
private val RdSubText = Color(0xFFB0BEC5)
private val RdDivider = Color.White.copy(alpha = 0.08f)

// ─── Actual 实现 ──────────────────────────────────────────────────────────────

actual val isRemoteDesktopSupported: Boolean = true

@Composable
actual fun RemoteDesktopScreen(
    prefilledIp: String,
    onBack: () -> Unit
) {
    val vm: RemoteDesktopViewModel = viewModel()
    val connectState by vm.connectState.collectAsState()

    // 用 BLE 选好的 IP 初始化输入框
    LaunchedEffect(prefilledIp) {
        if (prefilledIp.isNotBlank()) vm.setIp(prefilledIp)
    }

    when (connectState) {
        RemoteConnectState.IDLE,
        RemoteConnectState.FAILED ->
            ScrcpyConfigDialog(vm = vm, onBack = onBack)

        RemoteConnectState.CONNECTING ->
            ConnectingScreen(onBack = { vm.resetToIdle() })

        RemoteConnectState.CONNECTED ->
            ScrcpyMirrorScreen(vm = vm, onBack = {
                vm.resetToIdle()
                onBack()
            })
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  配置弹窗
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScrcpyConfigDialog(vm: RemoteDesktopViewModel, onBack: () -> Unit) {
    val ip by vm.ip.collectAsState()
    val resolution by vm.resolution.collectAsState()
    val bitrate by vm.bitrate.collectAsState()
    val delay by vm.delay.collectAsState()
    val noControl by vm.noControl.collectAsState()
    val showNav by vm.showNav.collectAsState()
    val screenOff by vm.screenOff.collectAsState()
    val audioForward by vm.audioForward.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RdBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0F1827), RdSurface)))
                .border(1.dp, RdDivider, RoundedCornerShape(24.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 标题 ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(RdAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.DesktopWindows, null, tint = RdAccent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("远程桌面", color = RdText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("配置连接参数", color = RdSubText, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, null, tint = RdSubText, modifier = Modifier.size(18.dp))
                }
            }

            Divider(color = RdDivider)

            // ── 错误提示 ─────────────────────────────────────────────────────
            if (errorMsg != null) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RdError.copy(alpha = 0.12f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = RdError, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(errorMsg ?: "", color = RdError, fontSize = 12.sp)
                }
            }

            // ── IP 地址 ──────────────────────────────────────────────────────
            RdTextField(value = ip, onValueChange = vm::setIp, label = "设备 IP 地址（ADB 无线调试地址）")

            // ── 分辨率 ──────────────────────────────────────────────────────
            RdLabel("视频分辨率")
            RdDropdown(
                options = RESOLUTION_OPTIONS,
                selected = resolution,
                label = { it.label },
                onSelect = vm::setResolution
            )

            // ── 码率 ────────────────────────────────────────────────────────
            RdLabel("视频码率")
            RdDropdown(
                options = BITRATE_OPTIONS,
                selected = bitrate,
                label = { it.label },
                onSelect = vm::setBitrate
            )

            // ── 延迟 ────────────────────────────────────────────────────────
            RdLabel("延迟容忍")
            RdDropdown(
                options = DELAY_OPTIONS,
                selected = delay,
                label = { it.label },
                onSelect = vm::setDelay
            )

            Divider(color = RdDivider)

            // ── 开关选项 ─────────────────────────────────────────────────────
            RdSwitch("仅查看（禁止触控）", noControl, vm::setNoControl)
            AnimatedVisibility(visible = !noControl) {
                RdSwitch("显示虚拟导航栏", showNav, vm::setShowNav)
            }
            RdSwitch("息屏控制模式", screenOff, vm::setScreenOff)
            RdSwitch("音频转发", audioForward, vm::setAudioForward)

            // ── 开始按钮 ─────────────────────────────────────────────────────
            // 注意：直接调用 vm.startConnection()，连接逻辑在 viewModelScope 中执行，
            // 不依赖 Composition 生命周期，避免 ForgottenCoroutineScopeException
            Button(
                onClick = { vm.startConnection() },
                enabled = ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = RdAccent,
                    disabledBackgroundColor = RdCard
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF0A0F1E), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "开始连接",
                    color = Color(0xFF0A0F1E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  连接中动画页面（含返回按钮）
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ConnectingScreen(onBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(RdBg),
        contentAlignment = Alignment.Center
    ) {
        // 返回按钮（左上角）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = RdText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 居中动画
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(100.dp).scale(scale).clip(CircleShape)
                        .background(RdAccent.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier.size(70.dp).clip(CircleShape)
                        .background(RdAccent.copy(alpha = 0.12f))
                )
                Icon(Icons.Rounded.DesktopWindows, null, tint = RdAccent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("正在连接远程桌面...", color = RdText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text("ADB 连接 → 部署服务 → 建立串流", color = RdSubText, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                color = RdAccent,
                backgroundColor = RdCard,
                modifier = Modifier.width(200.dp).clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  镜像界面（SurfaceView 全屏）
// ═══════════════════════════════════════════════════════════════════════════════

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun ScrcpyMirrorScreen(vm: RemoteDesktopViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val noControl by vm.noControl.collectAsState()
    val showNav by vm.showNav.collectAsState()

    var scrcpyService by remember { mutableStateOf<ScrcpyService?>(null) }
    var currentSurface by remember { mutableStateOf<android.view.Surface?>(null) }
    var isStarted by remember { mutableStateOf(false) }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                scrcpyService = (binder as? ScrcpyService.LocalBinder)?.getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                scrcpyService = null
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, ScrcpyService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        onDispose {
            scrcpyService?.stopService()
            context.runCatching { unbindService(serviceConnection) }
        }
    }

    LaunchedEffect(scrcpyService, currentSurface) {
        val s = currentSurface
        val service = scrcpyService
        if (s != null && service != null) {
            val res = vm.resolution.value
            if (!isStarted) {
                isStarted = true
                val dl = vm.delay.value
                service.start(
                    surface = s,
                    serverAddr = "${ScrcpyService.LOCAL_IP}:${ScrcpyService.LOCAL_FORWARD_PORT}",
                    screenHeight = res.height,
                    screenWidth = res.width,
                    delay = dl.ms,
                    audioEnabled = vm.audioForward.value
                )
            } else {
                service.setParms(s, res.width, res.height)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // SurfaceView 渲染层
        AndroidView(
            factory = { ctx ->
                android.view.SurfaceView(ctx).apply {
                    holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            currentSurface = holder.surface
                        }
                        override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, hi: Int) {}
                        override fun surfaceDestroyed(h: android.view.SurfaceHolder) {
                            currentSurface = null
                        }
                    })
                    if (!noControl) {
                        setOnTouchListener { _, event ->
                            val isLandscape = width > height
                            scrcpyService?.touchEvent(event, isLandscape, width, height) ?: false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 悬浮菜单
        DraggableFloatingMenu(scrcpyService = scrcpyService, onExit = onBack)
    }
}

@Composable
private fun DraggableFloatingMenu(scrcpyService: ScrcpyService?, onExit: () -> Unit) {
    var offsetX by remember { mutableStateOf(50f) }
    var offsetY by remember { mutableStateOf(50f) }
    var expanded by remember { mutableStateOf(false) }
    var screenWidth by remember { mutableStateOf(1000) }
    var screenHeight by remember { mutableStateOf(2000) }

    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(300)
    )

    val menuItems = listOf(
        Pair(Icons.Rounded.ArrowBack, "返回") to { scrcpyService?.sendKeyEvent(android.view.KeyEvent.KEYCODE_BACK) },
        Pair(Icons.Rounded.Home, "主页") to { scrcpyService?.sendKeyEvent(android.view.KeyEvent.KEYCODE_HOME) },
        Pair(Icons.Rounded.Apps, "任务") to { scrcpyService?.sendKeyEvent(android.view.KeyEvent.KEYCODE_APP_SWITCH) },
        Pair(Icons.Rounded.PowerSettingsNew, "电源") to { scrcpyService?.sendKeyEvent(android.view.KeyEvent.KEYCODE_POWER) },
        Pair(Icons.Rounded.Close, "退出") to onExit
    )

    val fabSizePx = with(LocalDensity.current) { 56.dp.toPx() }
    val itemSizePx = with(LocalDensity.current) { 50.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenWidth = it.size.width
                screenHeight = it.size.height
            }
    ) {
        val margin = fabSizePx * 1.5f
        val isLeftEdge = offsetX < margin
        val isRightEdge = offsetX > screenWidth - fabSizePx - margin
        val isTopEdge = offsetY < margin
        val isBottomEdge = offsetY > screenHeight - fabSizePx - margin

        val (startAngle, sweepAngle) = when {
            isLeftEdge && isTopEdge -> 0.0 to PI / 2           // 左上角 -> 90度扇形
            isLeftEdge && isBottomEdge -> 3 * PI / 2 to PI / 2 // 左下角 -> 90度扇形
            isRightEdge && isTopEdge -> PI / 2 to PI / 2       // 右上角 -> 90度扇形
            isRightEdge && isBottomEdge -> PI to PI / 2        // 右下角 -> 90度扇形
            isLeftEdge -> -PI / 2 to PI                        // 左边缘 -> 180度半圆
            isRightEdge -> PI / 2 to PI                        // 右边缘 -> 180度半圆
            isTopEdge -> 0.0 to PI                             // 上边缘 -> 180度半圆
            isBottomEdge -> PI to PI                           // 下边缘 -> 180度半圆
            else -> 0.0 to 2 * PI                              // 屏幕中间 -> 360度全圆
        }

        // 全圆需要等分所有item，扇形/半圆需要留空首尾
        val isFullCircle = sweepAngle >= 2 * PI - 0.01
        val stepAngle = if (isFullCircle) sweepAngle / menuItems.size else sweepAngle / (menuItems.size - 1)
        
        // 动态调整散开半径：如果是圆或半圆，半径可以小一点(因为很分散)；如果是扇形，半径需要大一点避免重叠
        val radiusPx = with(LocalDensity.current) { 
            if (sweepAngle <= PI / 2 + 0.01) 140.dp.toPx() else 90.dp.toPx() 
        }

        // 渲染展开的菜单项 (直接放在最外层 Box，解决裁切消失的问题)
        menuItems.forEachIndexed { i, (item, action) ->
            val itemAngle = startAngle + i * stepAngle
            val childX = animProgress * radiusPx * cos(itemAngle)
            val childY = animProgress * radiusPx * sin(itemAngle)
            val alpha = animProgress

            if (alpha > 0.01f) {
                // 计算基于整个屏幕的绝对坐标
                val centerOffset = (fabSizePx - itemSizePx) / 2
                val targetX = offsetX + centerOffset + childX
                val targetY = offsetY + centerOffset + childY

                Box(
                    modifier = Modifier
                        .offset { IntOffset(targetX.roundToInt(), targetY.roundToInt()) }
                        .size(50.dp)
                        .graphicsLayer {
                            this.alpha = alpha
                            this.scaleX = 0.6f + 0.4f * alpha
                            this.scaleY = 0.6f + 0.4f * alpha
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF283655)) // 恢复之前的深蓝色
                        .border(1.dp, RdAccent.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            action()
                            expanded = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(item.first, contentDescription = item.second, tint = RdAccent, modifier = Modifier.size(20.dp))
                        Text(item.second, color = RdText, fontSize = 9.sp, fontWeight = FontWeight.Normal)
                    }
                }
            }
        }

        // 渲染悬浮球本体 (直接放在最外层 Box)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .border(2.dp, RdAccent.copy(alpha = if (expanded) 1f else 0.5f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidth - fabSizePx)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeight - fabSizePx)
                            expanded = false
                        }
                    )
                }
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (expanded) Icons.Rounded.Close else Icons.Rounded.MoreHoriz,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ─── 可复用 UI 组件 ───────────────────────────────────────────────────────────

@Composable
private fun RdTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = RdAccent.copy(alpha = 0.8f), fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = RdText, fontWeight = FontWeight.Medium),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = RdText,
            focusedBorderColor = RdAccent,
            unfocusedBorderColor = RdAccent.copy(alpha = 0.4f),
            backgroundColor = RdCard,
            cursorColor = RdAccent,
            focusedLabelColor = RdAccent,
            unfocusedLabelColor = RdSubText
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun RdLabel(text: String) {
    Text(text, color = RdSubText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun <T> RdDropdown(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = RdCard,
                contentColor = RdText
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, RdAccent.copy(alpha = 0.4f))
        ) {
            Text(label(selected), fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, tint = RdAccent, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(RdCard)
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = { onSelect(option); expanded = false }) {
                    Text(
                        label(option),
                        color = if (option == selected) RdAccent else RdText,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RdSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = RdText, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RdAccent,
                checkedTrackColor = RdAccent.copy(alpha = 0.4f),
                uncheckedThumbColor = RdSubText,
                uncheckedTrackColor = RdCard
            )
        )
    }
}
