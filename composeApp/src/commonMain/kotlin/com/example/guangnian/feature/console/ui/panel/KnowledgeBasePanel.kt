package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.feature.console.data.bean.KnowledgeEntity
import com.example.guangnian.feature.console.viewmodel.KnowledgeViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun KnowledgeBasePanel(
    onSetAddAction: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KnowledgeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val knowledgeData = uiState.knowledgeData
    val isLoading = uiState.isLoading

    var showDeleteConfirm by remember { mutableStateOf<KnowledgeEntity?>(null) }
    var showEditDialog by remember { mutableStateOf<KnowledgeEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Toast 状态
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onSetAddAction("新增知识库") {
            showAddDialog = true
        }
        viewModel.fetchKnowledgeList()
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
            if (!isLoading) {
                if (knowledgeData.list.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("暂无知识库数据", color = Color.LightGray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(knowledgeData.list) { knowledge ->
                            KnowledgeItemBox(
                                knowledge = knowledge,
                                onEdit = { showEditDialog = knowledge },
                                onDelete = { showDeleteConfirm = knowledge }
                            )
                        }
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

        showDeleteConfirm?.let { knowledge ->
            ConfirmDialog(
                title = "删除知识库",
                content = "确定要删除该知识点吗？此操作不可恢复。",
                onConfirm = {
                    viewModel.deleteKnowledge(knowledge)
                    showDeleteConfirm = null
                },
                onDismiss = { showDeleteConfirm = null }
            )
        }

        showEditDialog?.let { knowledge ->
            KnowledgeEditDialog(
                initialKnowledge = knowledge,
                title = "编辑知识库",
                onConfirm = { edited ->
                    viewModel.editKnowledge(edited)
                    showEditDialog = null
                },
                onDismiss = { showEditDialog = null }
            )
        }

        if (showAddDialog) {
            KnowledgeEditDialog(
                initialKnowledge = null,
                title = "新增知识库",
                onConfirm = { newKnowledge ->
                    viewModel.addKnowledge(newKnowledge)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
private fun KnowledgeItemBox(
    knowledge: KnowledgeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(
                    text = "Q: ${knowledge.question}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "A: ${knowledge.answer}",
                    color = Color(0xBBFFFFFF),
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "编辑",
                    color = Color(0xFF007AFF),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onEdit() }.padding(4.dp)
                )
                Text(
                    text = "删除",
                    color = Color(0xFFE53935),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onDelete() }.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = { Text(content, color = Color.DarkGray, fontSize = 14.sp) },
        backgroundColor = Color.White,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确定", color = Color(0xFF007AFF)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
        }
    )
}

@Composable
private fun KnowledgeEditDialog(
    initialKnowledge: KnowledgeEntity?,
    title: String,
    onConfirm: (KnowledgeEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var question by remember(initialKnowledge) { mutableStateOf(initialKnowledge?.question ?: "") }
    var answer by remember(initialKnowledge) { mutableStateOf(initialKnowledge?.answer ?: "") }

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
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "《 $title",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 2.dp)
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("问题 (Question)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("答案 (Answer)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    maxLines = 10,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val result = initialKnowledge?.apply {
                                this.question = question
                                this.answer = answer
                                this.updateTime = Clock.System.now().toEpochMilliseconds()
                            } ?: KnowledgeEntity().apply {
                                this.id = Clock.System.now().toEpochMilliseconds()
                                this.question = question
                                this.answer = answer
                            }
                            onConfirm(result)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007AFF)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("保存", color = Color.White)
                    }
                }
            }
        }
    }
}
