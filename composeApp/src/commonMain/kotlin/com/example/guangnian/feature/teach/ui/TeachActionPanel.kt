package com.example.guangnian.feature.teach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.feature.console.data.bean.TeachActionBean
import com.example.guangnian.feature.teach.viewmodel.TeachViewModel
import kotlinx.coroutines.delay

@Composable
fun TeachActionPanel(
    onSetAddAction: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeachViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionList = uiState.actionList
    val status = uiState.teachStatus
    val isLoading = uiState.isLoading

    val isRecording = status.recording || status.teach.recording

    var showDeleteConfirm by remember { mutableStateOf<TeachActionBean?>(null) }
    var showExecuteConfirm by remember { mutableStateOf<TeachActionBean?>(null) }
    var showRenameDialog by remember { mutableStateOf<TeachActionBean?>(null) }
    var showStartRecordDialog by remember { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchTeachStatus()
        viewModel.fetchTeachList()
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            onSetAddAction("结束录制") {
                viewModel.stopTeach()
            }
        } else {
            onSetAddAction("开始录制") {
                showStartRecordDialog = true
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
            if (actionList.isEmpty() && !isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("暂无示教动作数据", color = Color.LightGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(actionList) { action ->
                        TeachActionItem(
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
        showStartRecordDialog.let {
            if (it) {
                var name by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showStartRecordDialog = false },
                    title = { Text("开始录制", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("动作名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    containerColor = Color.White,
                    confirmButton = {
                        TextButton(onClick = {
                            if (name.isNotBlank()) {
                                viewModel.startTeach(name)
                                showStartRecordDialog = false
                            }
                        }) { Text("开始", color = Color(0xFF007AFF)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartRecordDialog = false }) { Text("取消", color = Color.Gray) }
                    }
                )
            }
        }

        showRenameDialog?.let { action ->
            var newName by remember { mutableStateOf(action.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("修改名称", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("新名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank() && newName != action.name) {
                            viewModel.renameTeach(action.name, newName)
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
                text = { Text("确定要删除示教动作 “${action.name}” 吗？", color = Color.DarkGray, fontSize = 14.sp) },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTeach(action.name)
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
                text = { Text("确定要执行示教动作 “${action.name}” 吗？", color = Color.DarkGray, fontSize = 14.sp) },
                containerColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.executeTeach(action.name)
                        showExecuteConfirm = null
                    }) { Text("确定", color = Color(0xFF007AFF)) }
                },
                dismissButton = {
                    TextButton(onClick = { showExecuteConfirm = null }) { Text("取消", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
private fun TeachActionItem(
    action: TeachActionBean,
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
                    text = action.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    if (action.createTime.isNotEmpty()) {
                        Text(
                            text = action.createTime,
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    if (action.consumingTime.isNotEmpty()) {
                        Text(
                            text = "耗时: ${action.consumingTime}",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                        )
                    }
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
