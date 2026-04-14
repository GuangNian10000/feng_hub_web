package com.example.guangnian.feature.robot.screen.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// 预留大模型切换、提示词配置、视觉分析开关UI
@Composable
fun AgentSettingsPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("大模型智能体参数配置", color = Color.LightGray, fontSize = 14.sp)
    }
}