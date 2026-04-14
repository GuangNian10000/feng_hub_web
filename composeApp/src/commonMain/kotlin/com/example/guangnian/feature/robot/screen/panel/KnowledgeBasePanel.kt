package com.example.guangnian.feature.robot.screen.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// 预留本地知识库导入、RAG检索状态展示UI
@Composable
fun KnowledgeBasePanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("本地私有知识库管理", color = Color.LightGray, fontSize = 14.sp)
    }
}