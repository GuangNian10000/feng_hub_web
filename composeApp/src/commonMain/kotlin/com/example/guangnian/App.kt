package com.example.guangnian

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import com.example.guangnian.feature.robot.screen.MainControlScreen

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // 渲染主控面板
        MainControlScreen()
    }
}