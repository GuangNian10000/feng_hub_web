package com.example.guangnian.feature.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    onNavigateToFace: () -> Unit = {}
) {
    val viewModel: RobotViewModel = koinViewModel()
    val ip by viewModel.ip.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val canConnect by viewModel.canConnect.collectAsState()
    val controlType by viewModel.controlType.collectAsState()

    var showTypeDialog by remember { mutableStateOf(false) }

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
                ip = ip,
                port = port,
                connectionState = connectionState,
                canConnect = canConnect,
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
                    onNavigateToFace = onNavigateToFace
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
    onIpChange: (String) -> Unit, onPortChange: (String) -> Unit, onConnectClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //缩小按钮尺寸，移除硬编码的最小宽度限制，使其自适应文本内容
        Button(
            onClick = onConnectClick,
            enabled = canConnect,
            modifier = Modifier.height(44.dp).wrapContentWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (connectionState == TcpState.CONNECTED) Color(0xFF00C853) else Color(0xFF2962FF),
                disabledContainerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                disabledContentColor = Color.LightGray
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(
                text = when (connectionState) {
                    TcpState.CONNECTING -> "连接中..."
                    TcpState.CONNECTED -> "已连接"
                    TcpState.DISCONNECTED -> "连接设备"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        //设置半透明背景以隔离底层图像
        OutlinedTextField(
            value = ip,
            onValueChange = onIpChange,
            label = { Text("IP", color = Color(0xFFB0BEC5), fontSize = 12.sp) },
            modifier = Modifier.weight(2f).fillMaxHeight(),
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, shadow = DefaultTextShadow),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF40C4FF),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.7f),
                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text("Port", color = Color(0xFFB0BEC5), fontSize = 12.sp) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, shadow = DefaultTextShadow),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF40C4FF),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.7f),
                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun FunctionGrid(
    modifier: Modifier = Modifier,
    controlType: ControlType,
    onNavigateToInteraction: () -> Unit,
    onNavigateToFace: () -> Unit
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
            modifier = Modifier.fillMaxHeight().wrapContentWidth(),
            targetIconSize = targetIconSize,
            fontSize = fontSize,
            onClick = onNavigateToInteraction
        )

        if (controlType == ControlType.ROBOT) {
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard("示教动作", Res.drawable.ic_teach, Modifier.fillMaxHeight().wrapContentWidth(), targetIconSize, fontSize)
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard("人脸识别", Res.drawable.ic_face, Modifier.fillMaxHeight().wrapContentWidth(), targetIconSize, fontSize, onClick = onNavigateToFace)
        }

        if (controlType == ControlType.DOG) {
            Spacer(modifier = Modifier.width(spacerWidth))
            MenuCard("视频监控", Res.drawable.ic_video, Modifier.fillMaxHeight().wrapContentWidth(), targetIconSize, fontSize)
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
            Text(
                text = "型号: ${if (controlType == ControlType.DOG) "户外巡检版" else "通用机器人版"}",
                color = Color(0xFFE0E0E0),
                fontSize = 12.sp,
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
            Text(
                text = "控制核心: TCP 直连",
                color = Color(0xFFE0E0E0),
                fontSize = 12.sp,
                style = TextStyle(shadow = DefaultTextShadow)
            )
        }
    }
}