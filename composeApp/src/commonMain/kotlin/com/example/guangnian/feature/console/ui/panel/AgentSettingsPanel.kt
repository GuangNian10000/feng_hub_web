package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.feature.console.data.bean.AgentEntity
import com.example.guangnian.feature.console.viewmodel.AgentViewModel
import kotlinx.coroutines.delay
import kotlin.time.Clock
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AgentSettingsPanel(
    onSetAddAction: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    agentViewModel: AgentViewModel = koinViewModel()
) {
    val uiState by agentViewModel.uiState.collectAsState()
    val agentData = uiState.agentData
    val isLoading = uiState.isLoading

    var showDeleteConfirm by remember { mutableStateOf<AgentEntity?>(null) }
    var showSelectConfirm by remember { mutableStateOf<AgentEntity?>(null) }
    var showEditDialog by remember { mutableStateOf<AgentEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 跨平台 Toast 状态
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onSetAddAction("新增智能体") {
            showAddDialog = true
        }
        agentViewModel.fetchAgentList()
    }

    LaunchedEffect(Unit) {
        agentViewModel.toastEvent.collect { msg ->
            if (msg.isNotEmpty()) {
                toastMessage = msg
            }
        }
    }

    // 控制 Toast 显示时间
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000L)
            toastMessage = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (!isLoading) {
                if (agentData.list.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("暂无智能体数据", color = Color.LightGray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(agentData.list) { agent ->
                            val isSelected = agent.id == agentData.agent.id
                            AgentItemBox(
                                agent = agent,
                                isSelected = isSelected,
                                onSelect = { if (!isSelected) showSelectConfirm = agent },
                                onEdit = { showEditDialog = agent },
                                onDelete = { showDeleteConfirm = agent }
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

        // 跨平台 Toast UI 层
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

        showSelectConfirm?.let { agent ->
            ConfirmDialog(
                title = "切换智能体",
                content = "确定要应用智能体【${agent.name}】吗？",
                onConfirm = {
                    agentViewModel.selectAgent(agent)
                    showSelectConfirm = null
                },
                onDismiss = { showSelectConfirm = null }
            )
        }

        showDeleteConfirm?.let { agent ->
            ConfirmDialog(
                title = "删除智能体",
                content = "确定要删除智能体【${agent.name}】吗？此操作不可恢复。",
                onConfirm = {
                    agentViewModel.deleteAgent(agent)
                    showDeleteConfirm = null
                },
                onDismiss = { showDeleteConfirm = null }
            )
        }

        showEditDialog?.let { agent ->
            AgentEditDialog(
                initialAgent = agent,
                title = "编辑智能体",
                onConfirm = { editedAgent ->
                    agentViewModel.editAgent(editedAgent)
                    showEditDialog = null
                },
                onDismiss = { showEditDialog = null }
            )
        }

        if (showAddDialog) {
            AgentEditDialog(
                initialAgent = null,
                title = "新增智能体",
                onConfirm = { newAgent ->
                    agentViewModel.addAgent(newAgent)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentItemBox(
    agent: AgentEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF007AFF) else Color.Transparent
    val bgColor = if (isSelected) Color(0x4D007AFF) else Color(0x33FFFFFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选中指示器
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF007AFF),
                        unselectedColor = Color.LightGray
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            // 信息区
            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = agent.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "音色: ${agent.voice.sex.ifEmpty { "默认" }}",
                        color = Color(0xFFAAAAAA),
                        fontSize = 10.sp
                    )
                }

                if (agent.promptWord.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "提示词: ${agent.promptWord}",
                        color = Color(0xBBFFFFFF),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // 操作按钮区
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
        containerColor = Color.White,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确定", color = Color(0xFF007AFF)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
        }
    )
}


@Composable
private fun AgentEditDialog(
    initialAgent: AgentEntity?,
    title: String,
    onConfirm: (AgentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    // 使用 key 确保切换编辑对象时状态重置
    var name by remember(initialAgent) { mutableStateOf(initialAgent?.name ?: "") }
    var prompt by remember(initialAgent) { mutableStateOf(initialAgent?.promptWord ?: "") }
    var selectedVoice by remember(initialAgent) {
        mutableStateOf(initialAgent?.voice ?: com.example.guangnian.feature.console.data.bean.VoiceBean())
    }

    var showVoiceSelection by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 禁用平台默认宽度限制
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 设置弹窗宽度为屏幕 90%
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

                // 名称与音色并排显示，缩小占用比例
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("智能体名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.Gray,
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    // 音色选择触控区
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedVoice.sex.ifEmpty { "未选择" },
                            onValueChange = {},
                            label = { Text("音色配置") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLabelColor = Color.Gray,
                                unfocusedLabelColor = Color.Gray
                            )
                        )
                        // 拦截点击事件唤起全屏选择器
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showVoiceSelection = true }
                        )
                    }
                }

                // 提示词输入框，增加高度与行数
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("系统提示词 (Prompt)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp), // 增加提示词输入框高度
                    maxLines = 15,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                // 底部操作按钮
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
                            // 构造结果对象并回传
                            val resultAgent = initialAgent?.apply {
                                this.name = name
                                this.promptWord = prompt
                                this.voice = selectedVoice
                                this.updateTime = Clock.System.now().toEpochMilliseconds()
                            } ?: AgentEntity().apply {
                                this.id = Clock.System.now().toEpochMilliseconds()
                                this.name = name
                                this.promptWord = prompt
                                this.voice = selectedVoice
                            }
                            onConfirm(resultAgent)
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

    // 全屏音色选择弹窗
    if (showVoiceSelection) {
        VoiceSelectionDialog(
            currentVoice = selectedVoice,
            onSelect = { voice ->
                selectedVoice = voice // 更新本地状态
                showVoiceSelection = false
            },
            onDismiss = { showVoiceSelection = false }
        )
    }
}

/**
 * 独立的音色选择弹窗
 * 使用 Dialog 和 DialogProperties 支持自定义尺寸
 */
@Composable
private fun VoiceSelectionDialog(
    currentVoice: com.example.guangnian.feature.console.data.bean.VoiceBean,
    onSelect: (com.example.guangnian.feature.console.data.bean.VoiceBean) -> Unit,
    onDismiss: () -> Unit
) {
    // 内部临时维护当前选中的项
    var tempSelectedVoice by remember { mutableStateOf(currentVoice) }

    // 分类与分页状态
    val categories = listOf("全部", "火山", "微软", "中文", "日文", "英文", "方言")
    var selectedCategory by remember { mutableStateOf("全部") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 20

    // 根据选择的分类动态过滤数据
    val filteredList = remember(selectedCategory) {
        val allVoices = com.example.guangnian.feature.console.data.bean.StaticVoiceData.allVoices
        allVoices.filter { voice ->
            val lang = voice.language
            val sourceStr = voice.source.toString()
            when (selectedCategory) {
                "火山" -> sourceStr.contains("VOLCANO")
                "微软" -> sourceStr.contains("MICROSOFT")
                "中文" -> lang.contains("中") || lang.contains("粤") || lang.contains("台") || lang.contains("吴")
                "日文" -> lang.contains("日")
                "英文" -> lang.contains("英")
                "方言" -> lang.contains("粤") || lang.contains("台") || lang.contains("吴") ||
                        lang.contains("官话") || lang.contains("川") || lang.contains("闽") ||
                        lang.contains("客") || lang.contains("广西")
                else -> true // "全部"
            }
        }
    }

    // 计算总页数
    val totalPages = maxOf(1, (filteredList.size + pageSize - 1) / pageSize)

    // 切割当前页的数据
    val pagedList = remember(filteredList, currentPage) {
        filteredList.drop(currentPage * pageSize).take(pageSize)
    }

    // 切换分类时，页码重置为第 0 页
    LaunchedEffect(selectedCategory) {
        currentPage = 0
    }

    // usePlatformDefaultWidth = false 配合 fillMaxSize() 实现全屏
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(), // 强制全屏
            color = Color.White // 全屏背景改为白色
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // 1. 顶部标题
                Text(
                    text = "《 选择音色",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDismiss() }
                        .padding(bottom = 12.dp, end = 16.dp, top = 2.dp, start = 2.dp)
                )

                // 2. 动态分类筛选栏 (LazyRow横向滚动)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF007AFF) else Color(0x0A000000))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(category, color = if (isSelected) Color.White else Color.Black, fontSize = 14.sp)
                        }
                    }
                }

                // 3. 音色列表 (占满中间剩余空间)
                if (pagedList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("该分类下暂无音色", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pagedList) { voice ->
                            // 通过 ID 判断是否选中，每次进来能准确回显默认值
                            val isSelected = voice.id == tempSelectedVoice.id ||
                                    (tempSelectedVoice.id.isEmpty() && voice.sex == tempSelectedVoice.sex)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x1A007AFF) else Color(0x0A000000))
                                    .border(1.dp, if (isSelected) Color(0xFF007AFF) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { tempSelectedVoice = voice }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelectedVoice = voice },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF), unselectedColor = Color.LightGray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(text = voice.sex.ifEmpty { "默认" }, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "平台: ${voice.source} | 语言: ${voice.language}", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 底部控制区 (分页按钮 + 确认取消操作)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分页控制
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0
                        ) {
                            Text("上一页", color = if (currentPage > 0) Color(0xFF007AFF) else Color.Gray)
                        }

                        Text(
                            text = "${currentPage + 1} / $totalPages",
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontSize = 14.sp
                        )

                        TextButton(
                            onClick = { if (currentPage < totalPages - 1) currentPage++ },
                            enabled = currentPage < totalPages - 1
                        ) {
                            Text("下一页", color = if (currentPage < totalPages - 1) Color(0xFF007AFF) else Color.Gray)
                        }
                    }

                    // 操作按钮
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("取消", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSelect(tempSelectedVoice) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                        ) {
                            Text("确定选择", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}