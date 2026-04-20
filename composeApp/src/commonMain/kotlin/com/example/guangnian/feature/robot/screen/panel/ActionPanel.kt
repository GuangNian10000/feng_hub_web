package com.example.guangnian.feature.robot.screen.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.feature.robot.data.bean.ArmActionBean

@Composable
fun ActionPanel(
    armActions: List<ArmActionBean>,
    onActionClick: (Int) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (armActions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无姿态动作数据", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onRetryClick() }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("点击重试", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(armActions) { posture ->
                ActionItem(
                    text = posture.arm_name,
                    onClick = { onActionClick(posture.arm_id) }
                )
            }
        }
    }
}

@Composable
private fun ActionItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}