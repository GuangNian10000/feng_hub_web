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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
        InspectionPointEditDialog(
            initialPoint = showEditDialog,
            onConfirm = { point ->
                if (point.pointId.isEmpty()) {
                    onAddCurrentPosition(point)
                } else {
                    onUpdatePoint(point)
                }
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val newPoint = InspectionPointBean(
                        name = "点位_${endpoints.size + 1}",
                        position = com.example.guangnian.feature.robot.data.bean.PointPosition(
                            x = robotPosition?.x ?: 0.0,
                            y = robotPosition?.y ?: 0.0,
                            yaw = robotPosition?.yaw ?: 0.0
                        ),
                        x = robotPosition?.x ?: 0.0,
                        y = robotPosition?.y ?: 0.0,
                        yaw = robotPosition?.yaw ?: 0.0
                    )
                    showEditDialog = newPoint
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DC6B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加导览点", fontSize = 12.sp)
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
            val pos = point.position ?: com.example.guangnian.feature.robot.data.bean.PointPosition(point.x, point.y, point.yaw)
            Text(
                text = "类型: ${getPointTypeLabel(point.pointType)} | X: ${pos.x.format(2)} Y: ${pos.y.format(2)}",
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

@Composable
private fun InspectionPointEditDialog(
    initialPoint: InspectionPointBean?,
    onConfirm: (InspectionPointBean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialPoint) { mutableStateOf(initialPoint?.name ?: "") }
    val initialPos = initialPoint?.position
    var x by remember(initialPoint) { mutableStateOf(initialPos?.x?.toString() ?: initialPoint?.x?.toString() ?: "0.0") }
    var y by remember(initialPoint) { mutableStateOf(initialPos?.y?.toString() ?: initialPoint?.y?.toString() ?: "0.0") }
    var yaw by remember(initialPoint) { mutableStateOf(initialPos?.yaw?.toString() ?: initialPoint?.yaw?.toString() ?: "0.0") }
    var broadcastContent by remember(initialPoint) { mutableStateOf(initialPoint?.broadcastContent ?: "") }
    var playOnArrival by remember(initialPoint) { mutableStateOf(initialPoint?.playOnArrival ?: false) }
    var photoOnArrival by remember(initialPoint) { mutableStateOf(initialPoint?.photoOnArrival ?: false) }
    var pointType by remember(initialPoint) { mutableStateOf(initialPoint?.pointType ?: 0) }
    var stayTime by remember(initialPoint) { mutableStateOf(initialPoint?.stayTime?.toString() ?: "0.0") }
    var needRotate by remember(initialPoint) { mutableStateOf(initialPoint?.needRotate ?: false) }

    var showTypeMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2C2C2C)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isEdit = initialPoint?.pointId?.isNotEmpty() == true
                Text(if (isEdit) "编辑导览点" else "添加导览点", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                // 1. 基本信息 (名称 + 类型)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("点位名称") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = getPointTypeLabel(pointType),
                            onValueChange = {},
                            label = { Text("点位类型") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = Color.White) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showTypeMenu = true })
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                            modifier = Modifier.background(Color(0xFF3D3D3D))
                        ) {
                            listOf(0, 1, 2).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(getPointTypeLabel(type), color = Color.White) },
                                    onClick = {
                                        pointType = type
                                        showTypeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. 坐标信息 (X, Y, Yaw)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = x,
                        onValueChange = { x = it },
                        label = { Text("X 坐标") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = y,
                        onValueChange = { y = it },
                        label = { Text("Y 坐标") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = yaw,
                        onValueChange = { yaw = it },
                        label = { Text("角度 (Yaw)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                // 3. 停留时间与开关
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = stayTime,
                        onValueChange = { stayTime = it },
                        label = { Text("停留时间 (s)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = needRotate, onCheckedChange = { needRotate = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF007AFF)))
                            Text("需要旋转", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // 4. 广播内容
                OutlinedTextField(
                    value = broadcastContent,
                    onValueChange = { broadcastContent = it },
                    label = { Text("广播内容") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                // 5. 动作开关
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = playOnArrival, onCheckedChange = { playOnArrival = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF007AFF)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("到达播放", color = Color.White, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = photoOnArrival, onCheckedChange = { photoOnArrival = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF007AFF)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("到达拍照", color = Color.White, fontSize = 12.sp)
                    }
                }

                // 底部按钮
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val updated = (initialPoint ?: InspectionPointBean()).copy(
                                name = name,
                                position = com.example.guangnian.feature.robot.data.bean.PointPosition(
                                    x = x.toDoubleOrNull() ?: 0.0,
                                    y = y.toDoubleOrNull() ?: 0.0,
                                    yaw = yaw.toDoubleOrNull() ?: 0.0
                                ),
                                x = x.toDoubleOrNull() ?: 0.0, // 同时更新顶层字段以保持兼容
                                y = y.toDoubleOrNull() ?: 0.0,
                                yaw = yaw.toDoubleOrNull() ?: 0.0,
                                broadcastContent = broadcastContent,
                                playOnArrival = playOnArrival,
                                photoOnArrival = photoOnArrival,
                                pointType = pointType,
                                stayTime = stayTime.toDoubleOrNull() ?: 0.0,
                                needRotate = needRotate
                            )
                            onConfirm(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("保存配置", color = Color.White)
                    }
                }
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

