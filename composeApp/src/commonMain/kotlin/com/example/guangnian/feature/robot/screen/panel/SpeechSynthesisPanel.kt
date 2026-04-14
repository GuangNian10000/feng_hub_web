package com.example.guangnian.feature.robot.screen.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// 预留音色切换、语速调节、快捷语音发送UI
@Composable
fun SpeechSynthesisPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("语音合成与播报设置", color = Color.LightGray, fontSize = 14.sp)
    }
}