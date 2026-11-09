package com.example.guangnian.feature.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.core.network.ws.CommunicationFramework
import com.example.guangnian.core.network.ws.ServerMode
import com.example.guangnian.feature.blue.isBleSupported
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import com.example.guangnian.feature.main.viewmodel.ControlType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import feng_hub.composeapp.generated.resources.*

//全局定义文字阴影，增强复杂背景下的对比度
val DefaultTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.8f),
    offset = Offset(2f, 2f),
    blurRadius = 6f
)

@Composable
fun MainControlScreen(
    onNavigateToInteraction: () -> Unit = {},
    onNavigateToFace: () -> Unit = {},
    onNavigateToBle: () -> Unit = {}
) {
    val viewModel: RobotViewModel = koinViewModel()
    val framework by viewModel.framework.collectAsState()
    val serverMode by viewModel.serverMode.collectAsState()
    val ip by viewModel.ip.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val latency by viewModel.latency.collectAsState()
    val canConnect by viewModel.canConnect.collectAsState()
    val controlType by viewModel.controlType.collectAsState()

    var showTypeDialog by remember { mutableStateOf(false) }

    MainControlContent(
        framework = framework,
        serverMode = serverMode,
        ip = ip,
        port = port,
        connectionState = connectionState,
        connectionError = connectionError,
        latency = latency,
        canConnect = canConnect,
        controlType = controlType,
        onFrameworkChange = viewModel::setFramework,
        onServerModeChange = viewModel::setServerMode,
        onIpChange = viewModel::onIpChanged,
        onPortChange = viewModel::onPortChanged,
        onConnectClick = viewModel::connect,
        onNavigateToInteraction = onNavigateToInteraction,
        onNavigateToFace = onNavigateToFace,
        onNavigateToBle = onNavigateToBle,
        onTypeClick = { showTypeDialog = true }
    )

    if (showTypeDialog) {
        ControlTypeSelectionDialog(
            currentType = controlType,
            onDismiss = { showTypeDialog = false },
            onSelect = {
                viewModel.setControlType(it)
                showTypeDialog = false
            }
        )
    }
}

@Composable
fun MainControlContent(
    framework: CommunicationFramework = CommunicationFramework.NEW_FRAMEWORK,
    serverMode: ServerMode = ServerMode.MODE_A,
    ip: String,
    port: String,
    connectionState: TcpState,
    connectionError: String? = null,
    latency: Long? = null,
    canConnect: Boolean,
    controlType: ControlType,
    onFrameworkChange: (CommunicationFramework) -> Unit = {},
    onServerModeChange: (ServerMode) -> Unit = {},
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onNavigateToInteraction: () -> Unit,
    onNavigateToFace: () -> Unit,
    onNavigateToBle: () -> Unit = {},
    onTypeClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.bg_main),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            TopConnectionBar(
                framework = framework,
                serverMode = serverMode,
                ip = ip,
                port = port,
                connectionState = connectionState,
                connectionError = connectionError,
                canConnect = canConnect,
                onFrameworkChange = onFrameworkChange,
                onServerModeChange = onServerModeChange,
                onIpChange = onIpChange,
                onPortChange = onPortChange,
                onConnectClick = onConnectClick
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FunctionGrid(
                    controlType = controlType,
                    onNavigateToInteraction = onNavigateToInteraction,
                    onNavigateToFace = onNavigateToFace,
                    onNavigateToBle = onNavigateToBle
                )
            }

            BottomStatusBar(
                connectionState = connectionState,
                controlType = controlType,
                framework = framework,
                serverMode = serverMode,
                latency = latency,
                onTypeClick = onTypeClick
            )
        }
    }
}

@Composable
fun ControlTypeSelectionDialog(
    currentType: ControlType,
    onDismiss: () -> Unit,
    onSelect: (ControlType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择控制设备") },
        text = {
            Column {
                ControlType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(type) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = type == currentType,
                            onClick = { onSelect(type) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = type.displayName, fontSize = 18.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TopConnectionBar(
    framework: CommunicationFramework,
    serverMode: ServerMode,
    ip: String,
    port: String,
    connectionState: TcpState,
    connectionError: String?,
    canConnect: Boolean,
    onFrameworkChange: (CommunicationFramework) -> Unit,
    onServerModeChange: (ServerMode) -> Unit,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        // 第一行：通讯框架选择与平板端模式选择
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 框架选择
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "通讯框架:",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = DefaultTextShadow)
                )

                FrameworkChip(
                    title = "新框架 (TCP直连)",
                    selected = framework == CommunicationFramework.NEW_FRAMEWORK,
                    onClick = { onFrameworkChange(CommunicationFramework.NEW_FRAMEWORK) }
                )

                FrameworkChip(
                    title = "老框架 (WS双通道)",
                    selected = framework == CommunicationFramework.OLD_FRAMEWORK,
                    onClick = { onFrameworkChange(CommunicationFramework.OLD_FRAMEWORK) }
                )
            }

            // 当选择老框架时，显示平板端模式选择 (模式 A / 模式 B)
            if (framework == CommunicationFramework.OLD_FRAMEWORK) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "平板模式:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = DefaultTextShadow)
                    )

                    ServerModeChip(
                        title = "模式 A (透传/默认)",
                        selected = serverMode == ServerMode.MODE_A,
                        onClick = { onServerModeChange(ServerMode.MODE_A) }
                    )

                    ServerModeChip(
                        title = "模式 B (全网关)",
                        selected = serverMode == ServerMode.MODE_B,
                        onClick = { onServerModeChange(ServerMode.MODE_B) }
                    )
                }
            }
        }

        // 第二行：连接操作与 IP / 端口输入
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isConnected = connectionState == TcpState.CONNECTED
            val isConnecting = connectionState == TcpState.CONNECTING

            val btnBgColor = when {
                isConnected -> Color(0xFF16A34A).copy(alpha = 0.85f)
                isConnecting -> Color(0xFF3B82F6).copy(alpha = 0.85f)
                else -> Color(0xFF0F172A).copy(alpha = 0.7f)
            }
            val btnContentColor = when {
                isConnected -> Color.White
                isConnecting -> Color.White
                else -> Color(0xFF00E5FF)
            }
            val btnBorder = when {
                isConnected -> BorderStroke(1.dp, Color(0xFF16A34A))
                isConnecting -> BorderStroke(1.dp, Color(0xFF3B82F6))
                else -> BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
            }

            Button(
                onClick = onConnectClick,
                enabled = canConnect,
                modifier = Modifier.height(52.dp).wrapContentWidth().align(Alignment.CenterVertically),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = btnBgColor,
                    disabledBackgroundColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                    contentColor = btnContentColor,
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(6.dp),
                border = btnBorder,
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                Text(
                    text = when (connectionState) {
                        TcpState.CONNECTING  -> "连接中..."
                        TcpState.CONNECTED   -> "断开"
                        TcpState.DISCONNECTED -> "连接"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    style = TextStyle(shadow = DefaultTextShadow)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // IP 参数
            val ipLabel = if (framework == CommunicationFramework.NEW_FRAMEWORK) {
                "服务端 IP (ARM 底盘)"
            } else {
                "平板端 IP (feng_voice 9090)"
            }

            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                label = { Text(ipLabel, color = Color(0xFF00E5FF).copy(alpha = 0.85f), fontSize = 11.sp) },
                modifier = Modifier.weight(2f).height(56.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, shadow = DefaultTextShadow),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                    backgroundColor = Color(0xFF020617).copy(alpha = 0.7f),
                    cursorColor = Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Port 参数
            val portLabel = if (framework == CommunicationFramework.NEW_FRAMEWORK) {
                "端口 (默认 9002)"
            } else {
                "端口 (默认 9090)"
            }

            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text(portLabel, color = Color(0xFF00E5FF).copy(alpha = 0.85f), fontSize = 11.sp) },
                modifier = Modifier.weight(1f).height(56.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, shadow = DefaultTextShadow),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                    backgroundColor = Color(0xFF020617).copy(alpha = 0.7f),
                    cursorColor = Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(6.dp)
            )
        }

        // 错误提示
        if (!connectionError.isNullOrBlank() && connectionState == TcpState.DISCONNECTED) {
            Text(
                text = "连接异常: $connectionError",
                color = Color(0xFFFF5252),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }
    }
}

@Composable
private fun FrameworkChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF0F172A).copy(alpha = 0.6f)
    val border = if (selected) Color(0xFF00E5FF) else Color(0xFF334155).copy(alpha = 0.5f)
    val textColor = if (selected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.75f)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = bg,
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) Color(0xFF00E5FF) else Color.Gray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }
    }
}

@Composable
private fun ServerModeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFFA855F7).copy(alpha = 0.25f) else Color(0xFF0F172A).copy(alpha = 0.6f)
    val border = if (selected) Color(0xFFA855F7) else Color(0xFF334155).copy(alpha = 0.5f)
    val textColor = if (selected) Color(0xFFE9D5FF) else Color.White.copy(alpha = 0.75f)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = bg,
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (selected) Color(0xFFA855F7) else Color.Gray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }
    }
}

@Composable
fun FunctionGrid(
    modifier: Modifier = Modifier,
    controlType: ControlType,
    onNavigateToInteraction: () -> Unit,
    onNavigateToFace: () -> Unit,
    onNavigateToBle: () -> Unit = {}
) {
    val isDog = controlType == ControlType.DOG
    val targetIconSize = if (isDog) 160.dp else 140.dp
    val fontSize = if (isDog) 18.sp else 16.sp
    val spacerWidth = if (isDog) 80.dp else 32.dp

    Row(
        modifier = modifier.fillMaxHeight().wrapContentWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuCard(
            title = "总控制",
            icon = Res.drawable.ic_control,
            modifier = Modifier.weight(1f, fill = false).fillMaxHeight(),
            targetIconSize = targetIconSize,
            fontSize = fontSize,
            onClick = onNavigateToInteraction
        )

        if (controlType == ControlType.ROBOT) {
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard("人脸识别", Res.drawable.ic_face, Modifier.weight(1f, fill = false).fillMaxHeight(), targetIconSize, fontSize, onClick = onNavigateToFace)
        }

        if (controlType == ControlType.DOG) {
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard("视频监控", Res.drawable.ic_video, Modifier.weight(1f, fill = false).fillMaxHeight(), targetIconSize, fontSize)
        }

        // BLE 管理入口（仅 Android 显示）
        if (isBleSupported) {
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard(
                title = "设备管理",
                icon = Res.drawable.ic_device_connection,
                modifier = Modifier.weight(1f, fill = false).fillMaxHeight(),
                targetIconSize = targetIconSize,
                fontSize = fontSize,
                onClick = onNavigateToBle
            )
        }
    }
}

@Composable
fun MenuCard(
    title: String,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    targetIconSize: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier
                .weight(1f, fill = false)
                .sizeIn(maxWidth = targetIconSize, maxHeight = targetIconSize)
                .fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(12.dp))

        //添加文字阴影以防边界模糊
        Text(
            text = title,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            style = TextStyle(shadow = DefaultTextShadow)
        )
    }
}

@Composable
fun BottomStatusBar(
    connectionState: TcpState,
    controlType: ControlType,
    framework: CommunicationFramework,
    serverMode: ServerMode,
    latency: Long?,
    onTypeClick: () -> Unit
) {
    val (statusText, statusColor) = when (connectionState) {
        TcpState.DISCONNECTED -> "未连接" to Color(0xFFFF5252)
        TcpState.CONNECTING -> "连接中..." to Color(0xFFFFC107)
        TcpState.CONNECTED -> "已连接" to Color(0xFF00E676)
    }

    val frameworkText = if (framework == CommunicationFramework.NEW_FRAMEWORK) {
        "新框架 (TCP直连)"
    } else {
        "老框架 (WS: ${serverMode.displayName})"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.clickable { onTypeClick() }
            ) {
                Text(
                    text = "当前控制: ${controlType.displayName}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = DefaultTextShadow)
                )
            }

            Text(
                text = "架构: $frameworkText",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (connectionState == TcpState.CONNECTED && latency != null) {
                Text(
                    text = "延迟: ${latency}ms",
                    color = if (latency < 100) Color(0xFF00E676) else Color(0xFFFFC107),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = DefaultTextShadow)
                )
            }

            Text(
                text = "连接状态: $statusText",
                color = statusColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }
    }
}