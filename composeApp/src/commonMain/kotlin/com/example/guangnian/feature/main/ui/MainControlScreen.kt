package com.example.guangnian.feature.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.guangnian.feature.blue.BleAutoConnectDialog
import com.example.guangnian.feature.blue.isBleSupported
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import com.example.guangnian.feature.main.viewmodel.ControlType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import guangnian.composeapp.generated.resources.*

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
    val ip by viewModel.ip.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val canConnect by viewModel.canConnect.collectAsState()
    val controlType by viewModel.controlType.collectAsState()

    var showTypeDialog by remember { mutableStateOf(false) }
    var showBleAutoConnect by remember { mutableStateOf(false) }

    MainControlContent(
        ip = ip,
        port = port,
        connectionState = connectionState,
        canConnect = canConnect,
        controlType = controlType,
        onIpChange = viewModel::onIpChanged,
        onPortChange = viewModel::onPortChanged,
        onConnectClick = viewModel::connect,
        onNavigateToInteraction = onNavigateToInteraction,
        onNavigateToFace = onNavigateToFace,
        onNavigateToBle = onNavigateToBle,
        onTypeClick = { showTypeDialog = true },
        onAutoConnectClick = { showBleAutoConnect = true }
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

    // BLE 自动连接弹窗（仅 Android 显示）
    if (showBleAutoConnect) {
        BleAutoConnectDialog(
            onDismiss = { showBleAutoConnect = false },
            onIpSelected = { ip ->
                viewModel.onIpChanged(ip)
                showBleAutoConnect = false
                viewModel.connect()
            }
        )
    }
}

@Composable
fun MainControlContent(
    ip: String,
    port: String,
    connectionState: TcpState,
    canConnect: Boolean,
    controlType: ControlType,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onNavigateToInteraction: () -> Unit,
    onNavigateToFace: () -> Unit,
    onNavigateToBle: () -> Unit = {},
    onTypeClick: () -> Unit,
    onAutoConnectClick: () -> Unit = {}
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
                ip = ip,
                port = port,
                connectionState = connectionState,
                canConnect = canConnect,
                onIpChange = onIpChange,
                onPortChange = onPortChange,
                onConnectClick = onConnectClick,
                onAutoConnectClick = onAutoConnectClick
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
    ip: String, port: String, connectionState: TcpState,
    canConnect: Boolean,
    onIpChange: (String) -> Unit, onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onAutoConnectClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 连接按钮
        val isConnected = connectionState == TcpState.CONNECTED
        val btnColor = if (isConnected) Color(0xFF16A34A) else Color(0xFF3B82F6)
        Button(
            onClick = onConnectClick,
            enabled = canConnect,
            modifier = Modifier.height(46.dp).wrapContentWidth().align(Alignment.Bottom),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = btnColor,
                disabledBackgroundColor = Color(0xFF1E293B),
                contentColor = Color.White,
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(
                text = when (connectionState) {
                    TcpState.CONNECTING  -> "连接中..."
                    TcpState.CONNECTED   -> "已连接"
                    TcpState.DISCONNECTED -> "手动连接"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }

        // 自动连接按钮（BLE，仅 Android 显示）
        if (isBleSupported) {
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onAutoConnectClick,
                modifier = Modifier.height(46.dp).wrapContentWidth().align(Alignment.Bottom),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                    contentColor = Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text(
                    text = "自动连接",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    style = TextStyle(shadow = DefaultTextShadow)
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))
        
        // IP 参数
        OutlinedTextField(
            value = ip,
            onValueChange = onIpChange,
            label = { Text("IP 地址", color = Color(0xFF00E5FF).copy(alpha = 0.8f), fontSize = 11.sp) },
            modifier = Modifier.weight(2f).height(56.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, shadow = DefaultTextShadow),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                backgroundColor = Color(0xFF020617).copy(alpha = 0.6f),
                cursorColor = Color(0xFF00E5FF)
            ),
            shape = RoundedCornerShape(6.dp)
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Port 参数
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text("端口", color = Color(0xFF00E5FF).copy(alpha = 0.8f), fontSize = 11.sp) },
            modifier = Modifier.weight(1f).height(56.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, shadow = DefaultTextShadow),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                backgroundColor = Color(0xFF020617).copy(alpha = 0.6f),
                cursorColor = Color(0xFF00E5FF)
            ),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

@Composable
fun FunctionGrid(
    modifier: Modifier = Modifier,
    controlType: ControlType,
    onNavigateToInteraction: () -> Unit,
    onNavigateToFace: () -> Unit,
    onNavigateToBle: () -> Unit = {},
    onNavigateToRemoteDesktop: () -> Unit = {}
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
            
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard("远程桌面", Res.drawable.ic_remote_desktop, Modifier.weight(1f, fill = false).fillMaxHeight(), targetIconSize, fontSize, onClick = onNavigateToRemoteDesktop)
        }

        if (controlType == ControlType.DOG) {
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard("视频监控", Res.drawable.ic_video, Modifier.weight(1f, fill = false).fillMaxHeight(), targetIconSize, fontSize)
        }

        // BLE 管理入口（仅 Android 显示）
        if (isBleSupported) {
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard(
                title = "设备连接",
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
    onTypeClick: () -> Unit
) {
    val (statusText, statusColor) = when (connectionState) {
        TcpState.DISCONNECTED -> "未连接" to Color(0xFFFF5252)
        TcpState.CONNECTING -> "连接中..." to Color(0xFFFFC107)
        TcpState.CONNECTED -> "已连接" to Color(0xFF00E676)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
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
        Column(horizontalAlignment = Alignment.End) {
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