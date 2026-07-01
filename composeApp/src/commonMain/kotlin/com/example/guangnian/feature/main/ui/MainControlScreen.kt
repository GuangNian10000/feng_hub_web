package com.example.guangnian.feature.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import guangnian.composeapp.generated.resources.*

@Composable
fun MainControlScreen(
    onNavigateToInteraction: () -> Unit = {}
) {
    val viewModel: RobotViewModel = koinViewModel()
    val ip by viewModel.ip.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val canConnect by viewModel.canConnect.collectAsState()

    MainControlContent(
        ip = ip,
        port = port,
        connectionState = connectionState,
        canConnect = canConnect,
        onIpChange = viewModel::onIpChanged,
        onPortChange = viewModel::onPortChanged,
        onConnectClick = viewModel::connect,
        onNavigateToInteraction = onNavigateToInteraction
    )
}

@Composable
fun MainControlContent(
    ip: String,
    port: String,
    connectionState: TcpState,
    canConnect: Boolean,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onNavigateToInteraction: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片
        Image(
            painter = painterResource(Res.drawable.bg_main),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 主内容层
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 12.dp)
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
            Spacer(modifier = Modifier.height(16.dp))
            FunctionGrid(
                modifier = Modifier.weight(1f),
                onNavigateToInteraction = onNavigateToInteraction
            )
            Spacer(modifier = Modifier.height(16.dp))
            BottomStatusBar(connectionState = connectionState)
        }
    }
}

// ---------------- 以下都是纯粹的无状态 UI 组件 ----------------

@Composable
fun TopConnectionBar(
    ip: String, port: String, connectionState: TcpState,
    canConnect: Boolean,
    onIpChange: (String) -> Unit, onPortChange: (String) -> Unit, onConnectClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onConnectClick,
            enabled = canConnect,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (connectionState == TcpState.CONNECTED) Color(0xFF4CAF50) else Color(0xFF007AFF),
                disabledContainerColor = Color(0xFF555555)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = when (connectionState) {
                    TcpState.CONNECTING -> "连接中..."
                    TcpState.CONNECTED -> "已连接"
                    TcpState.DISCONNECTED -> "连接机器狗"
                },
                fontWeight = FontWeight.Bold,
                color = if (connectionState == TcpState.DISCONNECTED) Color.White else Color.LightGray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedTextField(
            value = ip, onValueChange = onIpChange, label = { Text("IP", color = Color.Gray) },
            modifier = Modifier.weight(2f).height(60.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF007AFF), unfocusedBorderColor = Color.DarkGray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = port, onValueChange = onPortChange, label = { Text("Port", color = Color.Gray) },
            modifier = Modifier.weight(1f).height(60.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF007AFF), unfocusedBorderColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun FunctionGrid(
    modifier: Modifier = Modifier,
    onNavigateToInteraction: () -> Unit
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        // 总控制模块
        MenuCard("总控制", Res.drawable.ic_control, Modifier.weight(1f), onClick = onNavigateToInteraction)
        Spacer(modifier = Modifier.width(24.dp))
        // 示教动作模块
        MenuCard("示教动作", Res.drawable.ic_teach, Modifier.weight(1f))
        Spacer(modifier = Modifier.width(24.dp))
        // 人脸识别模块
        MenuCard("人脸识别", Res.drawable.ic_face, Modifier.weight(1f))
    }
}

@Composable
fun MenuCard(title: String, icon: DrawableResource, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.fillMaxHeight().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA2C2C2C)), // 稍微透明一点以显示背景
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = title,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomStatusBar(connectionState: TcpState) {
    val (statusText, statusColor) = when (connectionState) {
        TcpState.DISCONNECTED -> "未连接" to Color.Red
        TcpState.CONNECTING -> "连接中..." to Color(0xFFFFC107)
        TcpState.CONNECTED -> "已连接" to Color(0xFF4CAF50)
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("机器狗名称: ${if (connectionState == TcpState.CONNECTED) "FENG BOT" else "未连接"}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("型号: 户外巡检版", color = Color.Gray, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("连接状态: $statusText", color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("控制核心: TCP 直连", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
