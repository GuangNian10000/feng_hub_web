package com.example.guangnian.feature.robot.ui.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// 预留建图录制、PGM地图加载、导航打点UI
@Composable
fun MapPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("SLAM建图与导航打点", color = Color.LightGray, fontSize = 14.sp)
    }
}