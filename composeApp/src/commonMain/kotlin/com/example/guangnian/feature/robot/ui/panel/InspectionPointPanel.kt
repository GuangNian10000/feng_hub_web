package com.example.guangnian.feature.robot.ui.panel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.pow
import kotlin.math.round
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
    onUpdatePoint: (InspectionPointBean) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf<InspectionPointBean?>(null) }
    var showEditDialog by remember { mutableStateOf<InspectionPointBean?>(null) }

    if (showEditDialog != null) {
        var editName by remember { mutableStateOf(showEditDialog?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("编辑导览点", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF03DC6B),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            containerColor = Color(0xFF2C2C2C),
            confirmButton = {
                TextButton(onClick = {
                    showEditDialog?.let { 
                        onUpdatePoint(it.copy(name = editName))
                    }
                    showEditDialog = null
                }) {
                    Text("保存", color = Color(0xFF03DC6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("取消", color = Color.White)
                }
            }
        )
    }

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
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(endpoints) { point ->
                    InspectionPointLinearItem(
                        point = point,
                        onClick = { onPointClick(point.pointId) },
                        onEditClick = { showEditDialog = point },
                        onDeleteClick = { showDeleteConfirm = point }
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectionPointLinearItem(
    point: InspectionPointBean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val typeColor = when (point.pointType) {
        1 -> Color(0xFF64B5F6) // 迎宾点淡蓝色
        2 -> Color(0xFF81C784) // 充电点淡绿色
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point.name.ifEmpty { "未命名" },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "类型: ${getPointTypeLabel(point.pointType)} | X: ${point.x.format(2)} Y: ${point.y.format(2)}",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }

        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
        }
    }
}

private fun Double.format(digits: Int): String {
    val multiplier = 10.0.pow(digits)
    return (round(this * multiplier) / multiplier).toString()
}

private fun getPointTypeLabel(type: Int): String {
    return when (type) {
        1 -> "迎宾点"
        2 -> "充电点"
        else -> "普通点"
    }
}

