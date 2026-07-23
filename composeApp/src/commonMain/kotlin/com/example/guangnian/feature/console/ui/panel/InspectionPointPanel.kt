package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.pow
import kotlin.math.round
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.feature.console.data.bean.InspectionPointBean
import com.example.guangnian.feature.console.data.bean.RouteBean
import com.example.guangnian.core.designsystem.components.ModernDialogColors
import com.example.guangnian.core.designsystem.components.ModernDialogSection
import com.example.guangnian.core.designsystem.components.ModernFormDialog
import com.example.guangnian.core.designsystem.components.ModernToggleOption
import com.example.guangnian.core.designsystem.components.modernOutlinedTextFieldColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InspectionPointPanel(
    endpoints: List<InspectionPointBean>,
    routes: List<RouteBean>,
    robotPosition: com.example.guangnian.feature.console.data.bean.RobotPositionBean?,
    selectedTab: String,
    onPointClick: (String) -> Unit,
    onAddCurrentPosition: (InspectionPointBean) -> Unit,
    onDeletePoint: (String) -> Unit,
    onUpdatePoint: (InspectionPointBean) -> Unit,
    onRouteClick: (String) -> Unit,
    onAddRoute: (RouteBean) -> Unit,
    onDeleteRoute: (String) -> Unit,
    onUpdateRoute: (RouteBean) -> Unit,
    onRetryClick: () -> Unit,
    onSetAddAction: (String, () -> Unit) -> Unit,
    armActions: List<com.example.guangnian.feature.console.data.bean.ArmActionBean> = emptyList(),
    pendingPoint: InspectionPointBean? = null,
    onClearPendingPoint: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf<InspectionPointBean?>(null) }
    var showEditDialog by remember { mutableStateOf<InspectionPointBean?>(null) }

    var showRouteDeleteConfirm by remember { mutableStateOf<RouteBean?>(null) }
    var showRouteEditDialog by remember { mutableStateOf<RouteBean?>(null) }

    // 监听外部传入的待添加点位
    LaunchedEffect(pendingPoint) {
        if (pendingPoint != null) {
            showEditDialog = pendingPoint
            onClearPendingPoint()
        }
    }

    LaunchedEffect(endpoints.size, routes.size, robotPosition, selectedTab) {
        if (selectedTab == "导览点") {
            onSetAddAction("添加导览点") {
                val newPoint = InspectionPointBean(
                    name = "点位_${endpoints.size + 1}",
                    position = com.example.guangnian.feature.console.data.bean.PointPosition(
                        x = robotPosition?.x ?: 0.0,
                        y = robotPosition?.y ?: 0.0,
                        yaw = robotPosition?.yaw ?: 0.0
                    ),
                    x = robotPosition?.x ?: 0.0,
                    y = robotPosition?.y ?: 0.0,
                    yaw = robotPosition?.yaw ?: 0.0
                )
                showEditDialog = newPoint
            }
        } else {
            onSetAddAction("添加路线") {
                showRouteEditDialog = RouteBean(
                    route_name = "路线_${routes.size + 1}"
                )
            }
        }
    }

    if (showEditDialog != null) {
        InspectionPointEditDialog(
            initialPoint = showEditDialog,
            armActions = armActions,
            onConfirm = { point ->
                if (point.point_id.isEmpty()) {
                    onAddCurrentPosition(point)
                } else {
                    onUpdatePoint(point)
                }
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
        )
    }

    if (showRouteEditDialog != null) {
        RouteEditDialog(
            initialRoute = showRouteEditDialog,
            availablePoints = endpoints,
            onConfirm = { route ->
                if (route.route_id.isNullOrEmpty()) {
                    onAddRoute(route)
                } else {
                    onUpdateRoute(route)
                }
                showRouteEditDialog = null
            },
            onDismiss = { showRouteEditDialog = null }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除", color = ModernDialogColors.PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("确定要删除导览点 “${showDeleteConfirm?.name}” 吗？", color = ModernDialogColors.SecondaryText, fontSize = 14.sp) },
            backgroundColor = ModernDialogColors.Surface,
            shape = RoundedCornerShape(8.dp),
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm?.let { onDeletePoint(it.point_id) }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("删除", fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消", color = ModernDialogColors.SecondaryText, fontSize = 14.sp)
                }
            }
        )
    }

    if (showRouteDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showRouteDeleteConfirm = null },
            title = { Text("确认删除路线", color = ModernDialogColors.PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("确定要删除路线 “${showRouteDeleteConfirm?.route_name}” 吗？", color = ModernDialogColors.SecondaryText, fontSize = 14.sp) },
            backgroundColor = ModernDialogColors.Surface,
            shape = RoundedCornerShape(8.dp),
            confirmButton = {
                Button(
                    onClick = {
                        showRouteDeleteConfirm?.route_id?.let { onDeleteRoute(it) }
                        showRouteDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("删除", fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRouteDeleteConfirm = null }) {
                    Text("取消", color = ModernDialogColors.SecondaryText, fontSize = 14.sp)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
        if (selectedTab == "导览点") {
            if (endpoints.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无导览点数据", color = Color.Gray, fontSize = 14.sp)
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(endpoints) { point ->
                        InspectionPointLinearItem(
                            point = point,
                            onClick = { onPointClick(point.point_id) },
                            onEditClick = { showEditDialog = point },
                            onDeleteClick = { showDeleteConfirm = point }
                        )
                    }
                }
            }
        } else {
            if (routes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无路线数据", color = Color.Gray, fontSize = 14.sp)
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(routes) { route ->
                        RouteLinearItem(
                            route = route,
                            onClick = { route.route_id?.let { onRouteClick(it) } },
                            onEditClick = { showRouteEditDialog = route },
                            onDeleteClick = { showRouteDeleteConfirm = route }
                        )
                    }
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
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x12FFFFFF), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point.name.ifEmpty { "未命名" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val pos = point.position ?: com.example.guangnian.feature.console.data.bean.PointPosition(point.x, point.y, point.yaw)
            Text(
                text = "类型: ${getPointTypeLabel(point.pointType)} | X: ${pos.x.format(2)} Y: ${pos.y.format(2)}",
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }

        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
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
    armActions: List<com.example.guangnian.feature.console.data.bean.ArmActionBean>,
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
    var armId by remember(initialPoint) { mutableStateOf(initialPoint?.armId ?: "") }
    var armName by remember(initialPoint, armActions) { 
        mutableStateOf(armActions.find { it.arm_id == initialPoint?.armId }?.arm_name ?: initialPoint?.arm_name ?: "") 
    }
    var skipAllowed by remember(initialPoint) { mutableStateOf(initialPoint?.skipAllowed ?: true) }
    var playPriority by remember(initialPoint) { mutableStateOf(initialPoint?.playPriority?.toString() ?: "0") }

    var showTypeMenu by remember { mutableStateOf(false) }
    var showArmMenu by remember { mutableStateOf(false) }

    val isEdit = initialPoint?.point_id?.isNotEmpty() == true
    ModernFormDialog(
        title = if (isEdit) "编辑导览点" else "添加导览点",
        confirmText = "保存配置",
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(),
        onConfirm = {
            val updated = (initialPoint ?: InspectionPointBean()).copy(
                name = name,
                position = com.example.guangnian.feature.console.data.bean.PointPosition(
                    x = x.toDoubleOrNull() ?: 0.0,
                    y = y.toDoubleOrNull() ?: 0.0,
                    yaw = yaw.toDoubleOrNull() ?: 0.0
                ),
                x = x.toDoubleOrNull() ?: 0.0,
                y = y.toDoubleOrNull() ?: 0.0,
                yaw = yaw.toDoubleOrNull() ?: 0.0,
                broadcastContent = broadcastContent,
                playOnArrival = playOnArrival,
                photoOnArrival = photoOnArrival,
                pointType = pointType,
                stayTime = stayTime.toDoubleOrNull() ?: 0.0,
                needRotate = needRotate,
                armId = armId.ifEmpty { null },
                arm_name = armName,
                skipAllowed = skipAllowed,
                playPriority = playPriority.toIntOrNull() ?: 0
            )
            onConfirm(updated)
        }
    ) {
                ModernDialogSection("基本信息")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("点位名称") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = getPointTypeLabel(pointType),
                            onValueChange = {},
                            label = { Text("点位类型") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = ModernDialogColors.SecondaryText) },
                            colors = modernOutlinedTextFieldColors()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showTypeMenu = true })
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            listOf(0, 1, 2).forEach { type ->
                                DropdownMenuItem(
                                    onClick = {
                                        pointType = type
                                        showTypeMenu = false
                                    }
                                ) {
                                    Text(getPointTypeLabel(type), color = Color.Black)
                                }
                            }
                        }
                    }
                }

                ModernDialogSection("位置与停留")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = x,
                        onValueChange = { x = it },
                        label = { Text("X 坐标") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = y,
                        onValueChange = { y = it },
                        label = { Text("Y 坐标") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = yaw,
                        onValueChange = { yaw = it },
                        label = { Text("角度 (Yaw)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = stayTime,
                        onValueChange = { stayTime = it },
                        label = { Text("停留时间 (s)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = playPriority,
                        onValueChange = { playPriority = it },
                        label = { Text("播放优先级") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )
                }

                ModernDialogSection("到达行为")

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = armName.ifEmpty { if(armId.isNotEmpty()) "动作ID: $armId" else "无关联动作" },
                            onValueChange = {},
                            label = { Text("关联手臂动作") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = ModernDialogColors.SecondaryText) },
                            colors = modernOutlinedTextFieldColors()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showArmMenu = true })
                        DropdownMenu(
                            expanded = showArmMenu,
                            onDismissRequest = { showArmMenu = false },
                            modifier = Modifier.background(Color.White).fillMaxWidth(0.4f).heightIn(max = 300.dp)
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    armId = ""
                                    armName = ""
                                    showArmMenu = false
                                }
                            ) {
                                Text("无关联动作", color = Color.Gray)
                            }
                            armActions.forEach { action ->
                                DropdownMenuItem(
                                    onClick = {
                                        armId = action.arm_id
                                        armName = action.arm_name
                                        showArmMenu = false
                                    }
                                ) {
                                    Text(action.arm_name, color = Color.Black)
                                }
                            }
                        }
                    }

                    ModernToggleOption(
                        label = "需要旋转",
                        checked = needRotate,
                        onCheckedChange = { needRotate = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernToggleOption(
                        label = "允许跳过",
                        checked = skipAllowed,
                        onCheckedChange = { skipAllowed = it },
                        modifier = Modifier.weight(1f)
                    )
                    ModernToggleOption(
                        label = "到达播放",
                        checked = playOnArrival,
                        onCheckedChange = { playOnArrival = it },
                        modifier = Modifier.weight(1f)
                    )
                    ModernToggleOption(
                        label = "到达拍照",
                        checked = photoOnArrival,
                        onCheckedChange = { photoOnArrival = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                ModernDialogSection("播报内容")

                OutlinedTextField(
                    value = broadcastContent,
                    onValueChange = { broadcastContent = it },
                    label = { Text("广播内容") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(6.dp),
                    colors = modernOutlinedTextFieldColors()
                )

    }
}

@Composable
private fun RouteLinearItem(
    route: RouteBean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x12FFFFFF), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = route.route_name.ifEmpty { "未命名路线" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "点位数: ${route.points?.size ?: route.pointCount ?: 0} | 循环: ${route.loopCount}次",
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }

        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RouteEditDialog(
    initialRoute: RouteBean?,
    availablePoints: List<InspectionPointBean>,
    onConfirm: (RouteBean) -> Unit,
    onDismiss: () -> Unit
) {
    var route_name by remember(initialRoute) { mutableStateOf(initialRoute?.route_name ?: "") }
    var description by remember(initialRoute) { mutableStateOf(initialRoute?.description ?: "") }
    var loopCount by remember(initialRoute) { mutableStateOf(initialRoute?.loopCount?.toString() ?: "1") }
    
    // 解析已有的点位
    val initialPointIds = remember(initialRoute) {
        // 优先从 points 列表提取 ID，如果没有则看 point_ids 列表，最后看 pointIds 字符串
        initialRoute?.points?.map { it.point_id }
            ?: initialRoute?.point_ids
            ?: initialRoute?.pointIds?.split(",")?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
    var selectedPointIds by remember { mutableStateOf(initialPointIds) }
    var showPointSelector by remember { mutableStateOf(false) }

    if (showPointSelector) {
        PointSelectionDialog(
            availablePoints = availablePoints,
            onConfirm = { ids ->
                selectedPointIds = selectedPointIds + ids
                showPointSelector = false
            },
            onDismiss = { showPointSelector = false }
        )
    }

    val isEdit = initialRoute?.route_id?.isNotEmpty() == true
    ModernFormDialog(
        title = if (isEdit) "编辑路线" else "添加路线",
        confirmText = "保存路线",
        onDismiss = onDismiss,
        confirmEnabled = route_name.isNotBlank() && selectedPointIds.isNotEmpty(),
        onConfirm = {
            val updated = (initialRoute ?: RouteBean()).copy(
                route_name = route_name,
                description = description,
                loopCount = loopCount.toIntOrNull() ?: 1,
                point_ids = selectedPointIds,
                points = selectedPointIds.mapNotNull { id -> availablePoints.find { it.point_id == id } }
            )
            onConfirm(updated)
        }
    ) {
                ModernDialogSection("基本信息")

                OutlinedTextField(
                    value = route_name,
                    onValueChange = { route_name = it },
                    label = { Text("路线名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = modernOutlinedTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = loopCount,
                        onValueChange = { loopCount = it },
                        label = { Text("循环次数") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("路线描述") },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        colors = modernOutlinedTextFieldColors()
                    )
                }

                ModernDialogSection("路线点位")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "已选择 ${selectedPointIds.size} 个点位",
                        color = ModernDialogColors.SecondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showPointSelector = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = ModernDialogColors.Accent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(6.dp),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("添加点位", fontSize = 12.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ModernDialogColors.Surface, RoundedCornerShape(6.dp))
                        .border(1.dp, ModernDialogColors.Border, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    if (selectedPointIds.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("尚未选择点位", color = ModernDialogColors.SecondaryText, fontSize = 13.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedPointIds.forEachIndexed { index, id ->
                                val point = availablePoints.find { it.point_id == id }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ModernDialogColors.Canvas)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}.", color = ModernDialogColors.Accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = point?.name ?: "未知点位 ($id)",
                                        color = ModernDialogColors.PrimaryText,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        selectedPointIds = selectedPointIds.toMutableList().apply { removeAt(index) }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

    }
}

@Composable
private fun PointSelectionDialog(
    availablePoints: List<InspectionPointBean>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .heightIn(max = 600.dp)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = ModernDialogColors.Surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        "选择导览点 (可多选)",
                        color = ModernDialogColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = ModernDialogColors.Divider)

                // List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(ModernDialogColors.Canvas)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    availablePoints.forEach { point ->
                        val isSelected = selectedIds.contains(point.point_id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ModernDialogColors.Surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ModernDialogColors.Accent else ModernDialogColors.Border,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSelected) selectedIds.remove(point.point_id) else selectedIds.add(point.point_id)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (it == true) selectedIds.add(point.point_id) else selectedIds.remove(point.point_id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ModernDialogColors.Accent,
                                    uncheckedColor = ModernDialogColors.Border
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(point.name, color = ModernDialogColors.PrimaryText, fontSize = 15.sp)
                        }
                    }
                }

                Divider(color = ModernDialogColors.Divider)

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = ModernDialogColors.SecondaryText)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { onConfirm(selectedIds.toList()) },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = ModernDialogColors.Accent,
                            contentColor = Color.White,
                            disabledBackgroundColor = ModernDialogColors.Accent.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(6.dp),
                        enabled = selectedIds.isNotEmpty(),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
                    ) {
                        Text("添加选定 (${selectedIds.size})", fontWeight = FontWeight.Medium)
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
