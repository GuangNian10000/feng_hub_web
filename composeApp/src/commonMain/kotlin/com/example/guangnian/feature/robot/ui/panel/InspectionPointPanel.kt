package com.example.guangnian.feature.robot.ui.panel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guangnian.feature.robot.data.bean.InspectionPointBean
import com.example.guangnian.feature.robot.data.bean.PointPosition
import com.example.guangnian.feature.robot.viewmodel.ActionViewModel
import com.example.guangnian.feature.robot.viewmodel.MapViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InspectionPointPanel(
    endpoints: List<InspectionPointBean>,
    robotPosition: com.example.guangnian.feature.robot.data.bean.RobotPositionBean?,
    onPointClick: (String) -> Unit,
    onAddCurrentPosition: (InspectionPointBean) -> Unit,
    onDeletePoint: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf<InspectionPointBean?>(null) }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除", color = Color.White) },
            text = { Text("确定要删除导览点 “${showDeleteConfirm?.name}” 吗？", color = Color.LightGray) },
            containerColor = Color(0xFF2C2C2C),
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm?.let { onDeletePoint(it.pointId) }
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消", color = Color.White)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("导览点列表", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            
            Button(
                onClick = {
                    if (robotPosition != null) {
                        val newPoint = InspectionPointBean(
                            name = "点位_${endpoints.size + 1}",
                            x = robotPosition.x,
                            y = robotPosition.y,
                            yaw = robotPosition.yaw,
                            position = PointPosition(
                                x = robotPosition.x,
                                y = robotPosition.y,
                                yaw = robotPosition.yaw
                            )
                        )
                        onAddCurrentPosition(newPoint)
                    }
                },
                enabled = robotPosition != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DC6B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加当前位置", fontSize = 12.sp)
            }
        }

        if (endpoints.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无导览数据", color = Color.Gray, fontSize = 14.sp)
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
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(endpoints) { point ->
                    InspectionPointGridItem(
                        point = point,
                        onClick = { onPointClick(point.pointId) },
                        onLongClick = { showDeleteConfirm = point }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InspectionPointGridItem(
    point: InspectionPointBean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val textColor = when (point.pointType) {
        1 -> Color(0xFF64B5F6) // 迎宾点淡蓝色
        2 -> Color(0xFF81C784) // 充电点淡绿色
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = point.name.ifEmpty { "未命名" },
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (point.pointType != 0) {
                Text(
                    text = getPointTypeLabel(point.pointType),
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun getPointTypeLabel(type: Int): String {
    return when (type) {
        1 -> "迎宾点"
        2 -> "充电点"
        else -> "普通点"
    }
}

