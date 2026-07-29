package com.example.guangnian.feature.motion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.feature.console.data.bean.ArmActionBean
import com.example.guangnian.feature.console.ui.components.PanelAddButton
import com.example.guangnian.feature.motion.viewmodel.ActionEditingViewModel
import com.example.guangnian.core.utils.KLog
import kotlinx.coroutines.delay

@Composable
fun ActionEditingPanel(
    onSetActions: (@Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActionEditingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionList = uiState.actionList
    val status = uiState.teachStatus
    val isLoading = uiState.isLoading

    val isRecordingState = rememberUpdatedState(status.recording || status.dds.recording)

    var showDeleteConfirm by remember { mutableStateOf<ArmActionBean?>(null) }
    var showExecuteConfirm by remember { mutableStateOf<ArmActionBean?>(null) }
    var showRenameDialog by remember { mutableStateOf<ArmActionBean?>(null) }
    var showSaveRecordDialog by remember { mutableStateOf(false) }

    var showStartConfirm by remember { mutableStateOf(false) }
    var showUnlockConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchActionEditingStatus()
        viewModel.fetchActionEditingList()
    }

    val armStatus = uiState.teachStatus.arm_status.takeIf { it.isNotEmpty() } ?: uiState.teachStatus.arm_status
    val armStatusState = rememberUpdatedState(armStatus)
    val showToastAction = remember { { msg: String -> toastMessage = msg } }

    LaunchedEffect(onSetActions) {
        onSetActions {
            val isRecording = isRecordingState.value
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showUnlockConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = com.example.guangnian.feature.console.ui.components.LockOpenIcon,
                        contentDescription = "解锁",
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { showRestoreConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = com.example.guangnian.feature.console.ui.components.LockIcon,
                        contentDescription = "恢复",
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                PanelAddButton(
                    text = if (isRecording) "结束录制" else "开始录制",
                    onClick = {
                        KLog.d("ActionEditingPanel", "录制按钮被点击, 当前isRecording=$isRecording")
                        if (isRecording) {
                            showSaveRecordDialog = true
                        } else {
                            if (armStatusState.value != "damping_unlock") {
                                showToastAction("当前未处于阻尼解锁状态，请先解锁")
                            } else {
                                showStartConfirm = true
                            }
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            if (msg.isNotEmpty()) {
                toastMessage = msg
            }
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000L)
            toastMessage = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
            
            if (armStatus.isNotEmpty()) {
                val statusText = when (armStatus) {
                    "damping_unlock" -> "已下发并维持手臂阻尼解锁"
                    "not_unlocked" -> "当前未处于阻尼解锁"
                    "unknown" -> "控制进程离线或状态无法确认"
                    else -> ""
                }
                if (statusText.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "手臂状态：$statusText",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (actionList.isEmpty() && !isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("暂无动作数据", color = Color.LightGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(actionList) { action ->
                        ActionEditingItem(
                            action = action,
                            onExecute = { showExecuteConfirm = action },
                            onRename = { showRenameDialog = action },
                            onDelete = { showDeleteConfirm = action }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xD9000000))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = toastMessage!!, color = Color.White, fontSize = 14.sp)
            }
        }

        // 弹窗逻辑
        if (showSaveRecordDialog) {
            var name by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                delay(100)
                focusRequester.requestFocus()
            }

            AlertDialog(
                onDismissRequest = { showSaveRecordDialog = false },
                title = { Text("保存录制", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("动作名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.stopActionEditing(name)
                            showSaveRecordDialog = false
                        } else {
                            toastMessage = "请输入动作名称"
                        }
                    }) { Text("保存", color = Color(0xFF007AFF)) }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveRecordDialog = false }) { Text("取消", color = Color.Gray) }
                }
            )
        }

        showRenameDialog?.let { action ->
            var newName by remember { mutableStateOf(action.arm_name) }
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                delay(100)
                focusRequester.requestFocus()
            }

            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("修改名称", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("新名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank() && newName != action.arm_name) {
                            viewModel.renameActionEditing(action.arm_name, newName)
                        }
                        showRenameDialog = null
                    }) { Text("保存", color = Color(0xFF007AFF)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) { Text("取消", color = Color.Gray) }
                }
            )
        }

        showDeleteConfirm?.let { action ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("确认删除", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = { Text("确定要删除动作 “${action.arm_name}” 吗？", color = Color.DarkGray, fontSize = 14.sp) },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteActionEditing(action.arm_name)
                        showDeleteConfirm = null
                    }) { Text("删除", color = Color(0xFFFF5252)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("取消", color = Color.Gray) }
                }
            )
        }

        showExecuteConfirm?.let { action ->
            AlertDialog(
                onDismissRequest = { showExecuteConfirm = null },
                title = { Text("确认执行", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = { Text("确定要执行动作 “${action.arm_name}” 吗？", color = Color.DarkGray, fontSize = 14.sp) },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.executeActionEditing(action.arm_id, action.arm_name)
                        showExecuteConfirm = null
                    }) { Text("确定", color = Color(0xFF007AFF)) }
                },
                dismissButton = {
                    TextButton(onClick = { showExecuteConfirm = null }) { Text("取消", color = Color.Gray) }
                }
            )
        }

        if (showStartConfirm) {
            AlertDialog(
                onDismissRequest = { showStartConfirm = false },
                title = { Text("确认开始", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = { Text("确认要开始录制动作吗？", color = Color.DarkGray, fontSize = 14.sp) },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.startActionEditing()
                        showStartConfirm = false
                    }) { Text("确定", color = Color(0xFF007AFF)) }
                },
                dismissButton = {
                    TextButton(onClick = { showStartConfirm = false }) { Text("取消", color = Color.Gray) }
                }
            )
        }

        if (showUnlockConfirm) {
            AlertDialog(
                onDismissRequest = { showUnlockConfirm = false },
                title = { Text("确认解锁", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = { Text("确认要下发阻尼解锁指令吗？", color = Color.DarkGray, fontSize = 14.sp) },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setSessionState("unlock")
                        showUnlockConfirm = false
                    }) { Text("确定", color = Color(0xFF007AFF)) }
                },
                dismissButton = {
                    TextButton(onClick = { showUnlockConfirm = false }) { Text("取消", color = Color.Gray) }
                }
            )
        }

        if (showRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                title = { Text("确认恢复", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = { Text("确认要下发恢复指令吗？", color = Color.DarkGray, fontSize = 14.sp) },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setSessionState("restore")
                        showRestoreConfirm = false
                    }) { Text("确定", color = Color(0xFF007AFF)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirm = false }) { Text("取消", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
private fun ActionEditingItem(
    action: ArmActionBean,
    onExecute: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .clickable { onExecute() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.arm_name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (action.time > 0) {
                    Text(
                        text = "耗时: ${action.time} s",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "修改",
                    color = Color(0xFF007AFF),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onRename() }.padding(4.dp)
                )
                Text(
                    text = "删除",
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onDelete() }.padding(4.dp)
                )
            }
        }
    }
}
