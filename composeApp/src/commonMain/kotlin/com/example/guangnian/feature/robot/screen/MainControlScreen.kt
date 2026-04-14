package com.example.guangnian.feature.robot.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.robot.viewmodel.RobotViewModel

@Composable
fun MainControlScreen(
    onNavigateToInteraction: () -> Unit = {}
) {
    val viewModel: RobotViewModel = viewModel { RobotViewModel() }
    val ip by viewModel.ip.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    MainControlContent(
        ip = ip,
        port = port,
        connectionState = connectionState,
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
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    onNavigateToInteraction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        TopConnectionBar(
            ip = ip,
            port = port,
            connectionState = connectionState,
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

// ---------------- 以下都是纯粹的无状态 UI 组件 ----------------

@Composable
fun TopConnectionBar(
    ip: String, port: String, connectionState: TcpState,
    onIpChange: (String) -> Unit, onPortChange: (String) -> Unit, onConnectClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onConnectClick,
            enabled = connectionState == TcpState.DISCONNECTED,
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
        // 绑定跳转回调至综合控制面板。
        MenuCard("综合控制", Modifier.weight(1f), onClick = onNavigateToInteraction)
        Spacer(modifier = Modifier.width(24.dp))
        MenuCard("建图录制", Modifier.weight(1f))
        Spacer(modifier = Modifier.width(24.dp))
        MenuCard("图传视频", Modifier.weight(1f))
    }
}

@Composable
fun MenuCard(title: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.fillMaxHeight().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFF444444), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("图", color = Color.Gray)
            }
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