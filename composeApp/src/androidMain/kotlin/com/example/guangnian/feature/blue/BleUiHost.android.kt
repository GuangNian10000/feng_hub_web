@file:Suppress("DEPRECATION")

package com.example.guangnian.feature.blue

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// ─── 设计令牌 ─────────────────────────────────────────────────────────────────

private val BleCardBg      = Color(0xFF131B2E)
private val BleSurface     = Color(0xFF1C2840)
private val BleAccent      = Color(0xFF40C4FF)
private val BleAccentDark  = Color(0xFF0288D1)
private val BleSuccess     = Color(0xFF00E676)
private val BleError       = Color(0xFFFF5252)
private val BleWarning     = Color(0xFFFFC107)
private val BleText        = Color.White
private val BleSubText     = Color(0xFFB0BEC5)
private val BleDivider     = Color.White.copy(alpha = 0.08f)

// ─── Platform ─────────────────────────────────────────────────────────────────

actual val isBleSupported: Boolean = true

// ═══════════════════════════════════════════════════════════════════════════════
//  BleAutoConnectDialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
actual fun BleAutoConnectDialog(
    onDismiss: () -> Unit,
    onIpSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: BleViewModel = viewModel()
    val autoScanState by viewModel.autoScanState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.bindService(context)
        delay(400L) // 给服务绑定一点时间
        viewModel.startAutoScan()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.unbindService(context) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0F1827), Color(0xFF131B2E)))
                )
                .border(1.dp, BleDivider, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                // ── 标题行 ──────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BleAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = BleAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "自动连接",
                            color = BleText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "搜索附近的语音交互端设备",
                            color = BleSubText,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = BleSubText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Divider(color = BleDivider)
                Spacer(Modifier.height(20.dp))

                // ── 内容区域（根据状态切换）───────────────────────────────────
                AnimatedContent(targetState = autoScanState, label = "scan_content") { state ->
                    when (state) {
                        is AutoScanState.Idle,
                        is AutoScanState.Scanning -> {
                            val text = if (state is AutoScanState.Scanning) state.step else "准备扫描..."
                            ScanningAnimationContent(text = text)
                        }
                        is AutoScanState.QueryingIp -> {
                            ScanningAnimationContent(
                                text = state.progress,
                                subText = "正在连接 ${state.device.name} 查询 IP..."
                            )
                        }
                        is AutoScanState.Done -> {
                            if (state.devices.isEmpty()) {
                                DialogErrorContent(
                                    message = "未找到任何语音交互端设备",
                                    onRetry = { viewModel.retryAutoScan() }
                                )
                            } else {
                                DeviceResultList(
                                    devices = state.devices,
                                    onIpSelected = { ip ->
                                        onIpSelected(ip)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                        is AutoScanState.Failed -> {
                            DialogErrorContent(
                                message = state.reason,
                                onRetry = { viewModel.retryAutoScan() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningAnimationContent(text: String, subText: String? = null) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 外环脉冲
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(BleAccent.copy(alpha = alpha * 0.3f))
            )
            // 中环
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(BleAccent.copy(alpha = 0.15f))
            )
            // 核心图标
            Icon(
                Icons.Rounded.BluetoothSearching,
                contentDescription = null,
                tint = BleAccent,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(text, color = BleText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (subText != null) {
            Spacer(Modifier.height(4.dp))
            Text(subText, color = BleSubText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DeviceResultList(
    devices: List<BleDevice>,
    onIpSelected: (String) -> Unit
) {
    var expandedAddresses by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "找到 ${devices.size} 台设备，点击 IP 地址即可连接",
            color = BleSuccess,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices, key = { it.address }) { device ->
                val isExpanded = device.address in expandedAddresses

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BleSurface)
                        .border(
                            1.dp,
                            if (device.ips.isNotEmpty()) BleAccent.copy(alpha = 0.4f) else BleDivider,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    // 设备行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (device.ips.isNotEmpty()) {
                                    expandedAddresses = if (isExpanded) {
                                        expandedAddresses - device.address
                                    } else {
                                        expandedAddresses + device.address
                                    }
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.BluetoothConnected,
                            contentDescription = null,
                            tint = if (device.ips.isNotEmpty()) BleAccent else BleSubText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                device.name,
                                color = BleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${device.address}  •  ${device.rssi} dBm",
                                color = BleSubText,
                                fontSize = 11.sp
                            )
                        }
                        if (device.ips.isNotEmpty()) {
                            Icon(
                                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = BleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("暂无 IP", color = BleSubText, fontSize = 11.sp)
                        }
                    }

                    // IP 列表（可展开）
                    AnimatedVisibility(
                        visible = isExpanded && device.ips.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Divider(color = BleDivider)
                            Spacer(Modifier.height(4.dp))
                            Text("可用 IP 地址", color = BleSubText, fontSize = 11.sp)
                            Spacer(Modifier.height(2.dp))
                            device.ips.forEach { ip ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BleAccent.copy(alpha = 0.12f))
                                        .clickable { onIpSelected(ip) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        ip,
                                        color = BleAccent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Icon(
                                        Icons.Rounded.TouchApp,
                                        contentDescription = "点击连接",
                                        tint = BleAccent.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.BluetoothDisabled,
            contentDescription = null,
            tint = BleError,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(message, color = BleSubText, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(backgroundColor = BleAccentDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("重新搜索", color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  BleManagementScreen
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
actual fun BleManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: BleViewModel = viewModel()
    val bleState       by viewModel.bleState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val logLines       by viewModel.logLines.collectAsState()

    LaunchedEffect(Unit) { viewModel.bindService(context) }
    DisposableEffect(Unit) { onDispose { viewModel.unbindService(context) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1E))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // ── 顶部导航栏 ────────────────────────────────────────────────────────
        BleManagementTopBar(bleState = bleState, onBack = onBack)

        // ── 主内容（横屏：左右分栏）─────────────────────────────────────────
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左栏：连接控制
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (val state = bleState) {
                    is BleState.Idle, is BleState.Error -> {
                        ScanDevicePanel(
                            viewModel      = viewModel,
                            scannedDevices = scannedDevices,
                            bleState       = bleState
                        )
                    }
                    is BleState.Scanning -> {
                        BleScanningPanel(step = state.step)
                    }
                    is BleState.Connecting -> {
                        BleConnectingPanel(device = state.device)
                    }
                    is BleState.Connected -> {
                        ConnectedCommandPanel(viewModel = viewModel, device = state.device, context = context)
                    }
                }
            }

            // 右栏：日志面板
            LogPanel(
                logLines = logLines,
                modifier = Modifier.weight(1f),
                onClear = viewModel::clearLog
            )
        }
    }
}

// ── 顶部导航栏 ────────────────────────────────────────────────────────────────

@Composable
private fun BleManagementTopBar(bleState: BleState, onBack: () -> Unit) {
    val (statusColor, statusText) = when (bleState) {
        is BleState.Idle        -> BleSubText   to "未连接"
        is BleState.Scanning    -> BleWarning   to "扫描中..."
        is BleState.Connecting  -> BleWarning   to "连接中..."
        is BleState.Connected   -> BleSuccess   to "已连接"
        is BleState.Error       -> BleError     to "错误"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1424))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = BleText)
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.BluetoothConnected, contentDescription = null, tint = BleAccent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text("BLE 设备管理", color = BleText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            Spacer(Modifier.width(6.dp))
            Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── 扫描面板（未连接时）──────────────────────────────────────────────────────

@Composable
private fun ScanDevicePanel(
    viewModel: BleViewModel,
    scannedDevices: List<BleDevice>,
    bleState: BleState
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 标题 + 扫描按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("附近设备", color = BleText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { viewModel.startScan() },
                colors = ButtonDefaults.buttonColors(backgroundColor = BleAccentDark),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("扫描设备", color = Color.White, fontSize = 13.sp)
            }
        }

        if (bleState is BleState.Error) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BleError.copy(alpha = 0.1f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = BleError, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(bleState.message, color = BleError, fontSize = 12.sp)
            }
        }

        if (scannedDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BleSurface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.BluetoothSearching, null, tint = BleSubText, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("点击扫描发现附近设备", color = BleSubText, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(scannedDevices, key = { it.address }) { device ->
                    ScannedDeviceItem(device = device, onConnect = { viewModel.connectDevice(device) })
                }
            }
        }
    }
}

@Composable
private fun ScannedDeviceItem(device: BleDevice, onConnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BleSurface)
            .border(1.dp, BleDivider, RoundedCornerShape(12.dp))
            .clickable { onConnect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BleAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bluetooth, null, tint = BleAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, color = BleText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "${device.address}  ${device.rssi} dBm",
                color = BleSubText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("连接", color = BleAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── 扫描中动画面板 ────────────────────────────────────────────────────────────

@Composable
private fun BleScanningPanel(step: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BleSurface)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BleAccent, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text(step, color = BleText, fontSize = 14.sp)
        }
    }
}

// ── 连接中面板 ────────────────────────────────────────────────────────────────

@Composable
private fun BleConnectingPanel(device: BleDevice) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BleSurface)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BleWarning, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text("正在连接 ${device.name}...", color = BleText, fontSize = 14.sp)
            Text(device.address, color = BleSubText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ── 已连接操作面板 ────────────────────────────────────────────────────────────

@Composable
private fun ConnectedCommandPanel(
    viewModel: BleViewModel,
    device: BleDevice,
    context: Context
) {
    var ssid     by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var customCmd by remember { mutableStateOf("") }
    var showWifiForm by remember { mutableStateOf(false) }
    var selectedTimeStr by remember { mutableStateOf("") }
    var selectedTimeDisplay by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BleSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 设备信息卡
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BleSuccess.copy(alpha = 0.08f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BleSuccess)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(device.name, color = BleSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(device.address, color = BleSubText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { viewModel.disconnect() },
                colors = ButtonDefaults.textButtonColors(contentColor = BleError)
            ) {
                Icon(Icons.Rounded.BluetoothDisabled, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("断开", fontSize = 12.sp)
            }
        }

        // 快捷操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BleActionButton(
                label = "查看 IP",
                icon = Icons.Rounded.Wifi,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.queryIp() }
            )
            BleActionButton(
                label = "查询时间",
                icon = Icons.Rounded.Schedule,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.queryTime() }
            )
        }

        // WiFi 配网折叠面板
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BleCardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWifiForm = !showWifiForm }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Wifi, null, tint = BleAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("下发 WiFi 配网", color = BleText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (showWifiForm) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = BleSubText, modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = showWifiForm) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BleTextField(value = ssid, onValueChange = { ssid = it }, label = "WiFi 名称 (SSID)")
                    BleTextField(value = password, onValueChange = { password = it }, label = "WiFi 密码")
                    Button(
                        onClick = {
                            if (ssid.isNotBlank()) viewModel.sendWifi(ssid, password)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                        shape = RoundedCornerShape(8.dp),
                        enabled = ssid.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.Send, null, tint = BleAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("下发配网", color = BleAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        // 设置系统时间
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BleCardBg)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccessTime, null, tint = BleAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("设置系统时间", color = BleText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showDateTimePicker(context) { disp, cmd ->
                        selectedTimeDisplay = disp; selectedTimeStr = cmd
                    }},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Rounded.DateRange, null, tint = BleAccent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (selectedTimeDisplay.isEmpty()) "选择时间" else selectedTimeDisplay,
                        color = BleAccent, fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { if (selectedTimeStr.isNotEmpty()) viewModel.setTime(selectedTimeStr) },
                    enabled = selectedTimeStr.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Rounded.CloudUpload, null, tint = if (selectedTimeStr.isEmpty()) BleSubText else BleAccent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("同步时间", color = if (selectedTimeStr.isEmpty()) BleSubText else BleAccent, fontSize = 12.sp)
                }
            }
        }

        // 自定义命令
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BleCardBg)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Terminal, null, tint = BleAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("自定义命令", color = BleText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                BleTextField(
                    value = customCmd,
                    onValueChange = { customCmd = it },
                    label = "Shell 命令",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { if (customCmd.isNotBlank()) { viewModel.execCmd(customCmd); customCmd = "" } },
                    enabled = customCmd.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = BleAccentDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── 日志面板 ──────────────────────────────────────────────────────────────────

@Composable
private fun LogPanel(
    logLines: List<String>,
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) listState.animateScrollToItem(logLines.lastIndex)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Article, null, tint = BleSubText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("回调日志", color = BleSubText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onClear,
                colors = ButtonDefaults.textButtonColors(contentColor = BleSubText),
                contentPadding = PaddingValues(4.dp)
            ) {
                Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("清空", fontSize = 11.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF080D18))
                .border(1.dp, BleDivider, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (logLines.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无日志", color = BleSubText.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logLines) { line ->
                        val color = when {
                            line.startsWith("!!") || line.startsWith("ERR") -> BleError
                            line.startsWith(">>") -> BleAccent
                            line.startsWith("OUT") -> Color(0xFFA5D6A7)
                            else -> BleSubText
                        }
                        Text(
                            line,
                            color = color,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ── 共享 UI 组件 ───────────────────────────────────────────────────────────────

@Composable
private fun BleActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(icon, null, tint = BleAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = BleAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = BleSubText, fontSize = 11.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyle(color = BleText, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = BleText,
            focusedBorderColor = BleAccent,
            unfocusedBorderColor = BleDivider,
            backgroundColor = BleCardBg,
            cursorColor = BleAccent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

// ── BleViewModel 扫描入口（兼容 BleManagementScreen 调用）────────────────────

private fun BleViewModel.startScan() = startAutoScan()

// ── 日期时间选择器辅助 ────────────────────────────────────────────────────────

private fun showDateTimePicker(
    context: Context,
    onSelected: (displayStr: String, cmdStr: String) -> Unit
) {
    val cal = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val display = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d:00", year, month + 1, day, hour, minute)
                    val cmd     = String.format(Locale.US, "%04d%02d%02d.%02d%02d00", year, month + 1, day, hour, minute)
                    onSelected(display, cmd)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
