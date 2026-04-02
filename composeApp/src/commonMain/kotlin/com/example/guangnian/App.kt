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

@Composable
@Preview
fun App() {
    // 采用户外巡检风格的深色主题
    MaterialTheme(colorScheme = darkColorScheme()) {
        // 最外层容器，填满全屏，深空灰背景
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .safeDrawingPadding() // 加入这一行：自动避开刘海、挖孔和圆角！
                .padding(horizontal = 24.dp, vertical = 12.dp) // 横屏时左右边距稍微留大点
        ){
            // 1. 顶部：连接控制栏
            TopConnectionBar()

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 中间：功能矩阵 (weight(1f) 会自动占据所有剩余的垂直空间)
            FunctionGrid(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 底部：机器狗状态栏
            BottomStatusBar()
        }
    }
}

@Composable
fun TopConnectionBar() {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 连接按钮 (使用工业警示色/高亮蓝)
        Button(
            onClick = { /* TODO: 机器狗连接逻辑 */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("连接机器狗", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // IP 输入框 (weight 2)
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP", color = Color.Gray) },
            modifier = Modifier.weight(2f).height(60.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF007AFF),
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 端口 输入框 (weight 1)
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port", color = Color.Gray) },
            modifier = Modifier.weight(1f).height(60.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF007AFF),
                unfocusedBorderColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun FunctionGrid(modifier: Modifier = Modifier) {
    // 横向排列三个卡片
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
        modifier = modifier
            .fillMaxHeight() // 卡片填满中间区域的高度
            .clickable { /* TODO: 页面跳转逻辑 */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 占位符：后续你可以将 @mipmap 的图片放进 composeApp/src/commonMain/composeResources/drawable
            // 然后用 Image(painterResource(Res.drawable.xxx), contentDescription = null) 替换这个 Box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF444444), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("图", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BottomStatusBar() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧：机器狗基本信息
        Column {
            Text("连接状态: 未连接", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("服务器: 未连接", color = Color.Gray, fontSize = 12.sp)
        }
    }
}