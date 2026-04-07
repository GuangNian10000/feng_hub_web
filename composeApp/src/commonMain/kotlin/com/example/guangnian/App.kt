package com.example.guangnian

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.robot.viewmodel.RobotViewModel

@Composable
@Preview
fun App() {
    val viewModel: RobotViewModel = viewModel { RobotViewModel() }

    // 收集 ViewModel 中的所有状态
    val ip by viewModel.ip.collectAsState()
    val port by viewModel.port.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // UI 组件变为了纯粹的无状态函数 (Stateless)
            TopConnectionBar(
                ip = ip,
                port = port,
                connectionState = connectionState,
                onIpChange = viewModel::onIpChanged,
                onPortChange = viewModel::onPortChanged,
                onConnectClick = viewModel::connect
            )

            Spacer(modifier = Modifier.height(16.dp))
            FunctionGrid(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            BottomStatusBar(connectionState = connectionState)
        }
    }
}

// ---------------- 以下都是纯粹的无状态 UI 组件 ----------------

@Composable
fun TopConnectionBar(
    ip: String,
    port: String,
    connectionState: TcpState,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit
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
            value = ip,
            onValueChange = onIpChange, // 直接把用户的输入丢给外部处理
            label = { Text("IP", color = Color.Gray) },
            modifier = Modifier.weight(2f).height(60.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF007AFF), unfocusedBorderColor = Color.DarkGray
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text("Port", color = Color.Gray) },
            modifier = Modifier.weight(1f).height(60.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF007AFF), unfocusedBorderColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun FunctionGrid(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuCard("综合控制", Modifier.weight(1f))
        Spacer(modifier = Modifier.width(24.dp))
        MenuCard("建图录制", Modifier.weight(1f))
        Spacer(modifier = Modifier.width(24.dp))
        MenuCard("图传视频", Modifier.weight(1f))
    }
}

@Composable
fun MenuCard(title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight().clickable { /* TODO */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFF444444), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Text("图", color = Color.Gray) }
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

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text("连接状态: $statusText", color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}