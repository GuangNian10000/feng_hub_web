package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.designsystem.components.ModernDialogColors
import com.example.guangnian.core.designsystem.components.ModernDialogSection
import com.example.guangnian.core.designsystem.components.ModernFormDialog
import com.example.guangnian.core.designsystem.components.modernOutlinedTextFieldColors
import com.example.guangnian.feature.console.data.api.VoiceApi
import com.example.guangnian.feature.console.data.bean.ArmActionBean
import com.example.guangnian.feature.console.data.bean.SpeechSynthesisBean
import com.example.guangnian.feature.console.data.bean.VoiceBean
import com.example.guangnian.feature.console.viewmodel.ConsoleViewModel
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.Clock

// ==========================================
// 1. ViewModel 与本地存储逻辑
// ==========================================

class SpeechSynthesisViewModel(
    private val voiceApi: VoiceApi,
    private val settings: Settings
) : ViewModel() {
    private companion object {
        const val TAG = "SpeechSynthesisViewModel"
    }

    private val _synthesisList = MutableStateFlow<List<SpeechSynthesisBean>>(emptyList())
    val synthesisList: StateFlow<List<SpeechSynthesisBean>> = _synthesisList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        loadFromSp()
    }

    // 加载本地 SP 数据
    private fun loadFromSp() {
        val jsonStr = settings.getString("speech_synthesis_list", "[]")
        try {
            val list = Json.decodeFromString<List<SpeechSynthesisBean>>(jsonStr)
            _synthesisList.value = list
            KLog.d(TAG, "读取本地语音合成列表成功: count=${list.size}, jsonLength=${jsonStr.length}")
        } catch (e: Exception) {
            KLog.w(TAG, "读取本地语音合成列表失败，使用空列表: jsonLength=${jsonStr.length}, error=${e.message}", e)
            _synthesisList.value = emptyList()
        }
    }

    // 保存至本地 SP
    private fun saveToSp(list: List<SpeechSynthesisBean>) {
        _synthesisList.value = list
        try {
            val jsonStr = Json.encodeToString(list)
            // TODO: 替换为实际的 KM Settings 写入逻辑
            settings.putString("speech_synthesis_list", jsonStr)
            KLog.d(TAG, "保存本地语音合成列表成功: count=${list.size}, jsonLength=${jsonStr.length}")
        } catch (e: Exception) {
            KLog.w(TAG, "保存本地语音合成列表失败: count=${list.size}, error=${e.message}", e)
        }
    }

    fun addOrUpdateItem(bean: SpeechSynthesisBean) {
        val currentList = _synthesisList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == bean.id }
        if (index >= 0) {
            currentList[index] = bean.copy(updateTime = Clock.System.now().toEpochMilliseconds())
        } else {
            currentList.add(
                0,
                bean.copy(
                    id = Clock.System.now().toEpochMilliseconds().toString(),
                    updateTime = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
        KLog.i(TAG, "保存语音合成条目: isUpdate=${index >= 0}, id=${bean.id}, textLength=${bean.text.length}, voiceId=${bean.voice?.id}, actionId=${bean.action?.arm_id}")
        saveToSp(currentList)
        viewModelScope.launch { _toastEvent.emit("保存成功") }
    }

    fun deleteItem(id: String) {
        val currentList = _synthesisList.value.toMutableList()
        val beforeCount = currentList.size
        currentList.removeAll { it.id == id }
        KLog.i(TAG, "删除语音合成条目: id=$id, before=$beforeCount, after=${currentList.size}")
        saveToSp(currentList)
        viewModelScope.launch { _toastEvent.emit("删除成功") }
    }

    // 执行合成（发送 TCP 请求）
    fun executeSynthesis(bean: SpeechSynthesisBean) {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            _isLoading.value = true
            try {
                KLog.i(TAG, "开始执行语音合成: id=${bean.id}, textLength=${bean.text.length}, voiceId=${bean.voice?.id}, actionId=${bean.action?.arm_id}")
                voiceApi.executeSynthesis(bean)
                KLog.d(TAG, "语音合成指令发送成功: id=${bean.id}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                _toastEvent.emit("指令已发送")
            } catch (e: Exception) {
                KLog.w(TAG, "语音合成指令发送失败: id=${bean.id}, error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                _toastEvent.emit("发送异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// ==========================================
// 2. UI 主面板
// ==========================================

@Composable
fun SpeechSynthesisPanel(
    onSetAddAction: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpeechSynthesisViewModel = koinViewModel(),
    consoleViewModel: ConsoleViewModel = koinViewModel() // 传入动作 ViewModel 用于获取动作列表
) {
    val listData by viewModel.synthesisList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showEditDialog by remember { mutableStateOf<SpeechSynthesisBean?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showConfirmExecute by remember { mutableStateOf<SpeechSynthesisBean?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onSetAddAction("新增语料") {
            showAddDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            if (msg.isNotEmpty()) toastMessage = msg
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
            // 列表区
            if (listData.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("暂无合成语料数据，请点击右上角新增", color = Color.LightGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listData) { item ->
                        SynthesisItemBox(
                            bean = item,
                            onExecute = { showConfirmExecute = item },
                            onEdit = { showEditDialog = item },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }

        // 全局 Loading
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // 跨平台 Toast
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

        // 弹窗控制
        showEditDialog?.let { bean ->
            SynthesisEditDialog(
                initialData = bean,
                title = "编辑语料",
                consoleViewModel = consoleViewModel,
                onConfirm = {
                    viewModel.addOrUpdateItem(it)
                    showEditDialog = null
                },
                onDismiss = { showEditDialog = null }
            )
        }

        if (showAddDialog) {
            SynthesisEditDialog(
                initialData = null,
                title = "新增语料",
                consoleViewModel = consoleViewModel,
                onConfirm = {
                    viewModel.addOrUpdateItem(it)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        if (showConfirmExecute != null) {
            AlertDialog(
                onDismissRequest = { showConfirmExecute = null },
                title = { Text("确认合成", color = Color.Black, fontWeight = FontWeight.Bold) },
                text = { Text("确定要执行语料 “${showConfirmExecute?.text}” 的合成吗？", color = Color.DarkGray) },
                backgroundColor = Color.White,
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmExecute?.let { viewModel.executeSynthesis(it) }
                        showConfirmExecute = null
                    }) {
                        Text("确定", color = Color(0xFF007AFF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmExecute = null }) {
                        Text("取消", color = Color.Gray)
                    }
                }
            )
        }
    }
}

// ==========================================
// 3. 列表项 UI
// ==========================================

@Composable
fun SynthesisItemBox(
    bean: SpeechSynthesisBean,
    onExecute: () -> Unit,
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
            // 信息区
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = bean.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (bean.voice != null) {
                        Text(
                            text = "音色: ${bean.voice!!.sex.ifEmpty { "自定义" }}",
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp,
                            modifier = Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (bean.action != null) {
                        Text(
                            text = "动作: ${bean.action!!.arm_name}",
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp,
                            modifier = Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 操作区
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onExecute,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007AFF)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("合成", color = Color.White, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("编辑", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.clickable { onEdit() })
                    Text("删除", color = Color(0xFFE53935), fontSize = 11.sp, modifier = Modifier.clickable { onDelete() })
                }
            }
        }
    }
}

// ==========================================
// 4. 编辑/新增弹窗
// ==========================================

@Composable
fun SynthesisEditDialog(
    initialData: SpeechSynthesisBean?,
    title: String,
    consoleViewModel: ConsoleViewModel,
    onConfirm: (SpeechSynthesisBean) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialData) { mutableStateOf(initialData?.text ?: "") }
    var selectedVoice by remember(initialData) { mutableStateOf(initialData?.voice) }
    var selectedAction by remember(initialData) { mutableStateOf(initialData?.action) }

    var showVoiceSelection by remember { mutableStateOf(false) }
    var showActionSelection by remember { mutableStateOf(false) }

    ModernFormDialog(
        title = title,
        confirmText = "保存",
        onDismiss = onDismiss,
        confirmEnabled = text.isNotBlank(),
        onConfirm = {
            val result = initialData?.let {
                it.text = text
                it.voice = selectedVoice
                it.action = selectedAction
                it
            } ?: SpeechSynthesisBean(
                text = text,
                voice = selectedVoice,
                action = selectedAction
            )
            onConfirm(result)
        }
    ) {
        ModernDialogSection("播报内容")

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("播报文本 (必填)", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 12,
            shape = RoundedCornerShape(6.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            colors = modernOutlinedTextFieldColors()
        )

        ModernDialogSection("附加配置")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedVoice?.sex?.ifEmpty { "已选择" } ?: "默认音色",
                    onValueChange = {},
                    label = { Text("指定音色", fontSize = 12.sp) },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    colors = modernOutlinedTextFieldColors()
                )
                Box(modifier = Modifier.matchParentSize().clickable { showVoiceSelection = true })
                if (selectedVoice != null) {
                    IconButton(
                        onClick = { selectedVoice = null },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除音色",
                            tint = Color(0xFFD14343)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "选择音色",
                        tint = ModernDialogColors.SecondaryText,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedAction?.arm_name ?: "无关联动作",
                    onValueChange = {},
                    label = { Text("指定动作", fontSize = 12.sp) },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    colors = modernOutlinedTextFieldColors()
                )
                Box(modifier = Modifier.matchParentSize().clickable { showActionSelection = true })
                if (selectedAction != null) {
                    IconButton(
                        onClick = { selectedAction = null },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除动作",
                            tint = Color(0xFFD14343)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "选择动作",
                        tint = ModernDialogColors.SecondaryText,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                    )
                }
            }
        }
    }

    if (showVoiceSelection) {
        SynthesisVoiceSelectionDialog(
            currentVoice = selectedVoice ?: VoiceBean(),
            onSelect = {
                selectedVoice = it
                showVoiceSelection = false
            },
            onDismiss = { showVoiceSelection = false }
        )
    }

    if (showActionSelection) {
        ActionSelectionDialog(
            consoleViewModel = consoleViewModel,
            currentActionId = selectedAction?.arm_id,
            onSelect = { armAction ->
                selectedAction = armAction
                showActionSelection = false
            },
            onDismiss = { showActionSelection = false }
        )
    }
}

// ==========================================
// 5. 动作选择弹窗 (全屏 - 仅手臂动作)
// ==========================================

@Composable
fun ActionSelectionDialog(
    consoleViewModel: ConsoleViewModel,
    currentActionId: String?,
    onSelect: (ArmActionBean) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by consoleViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.armActions.isEmpty()) consoleViewModel.fetchArmList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1E1E1E)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "《 选择动作",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDismiss() }
                        .padding(bottom = 16.dp, end = 16.dp, top = 2.dp, start = 2.dp)
                )

                // 动作列表 Grid
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                    } else {
                        val itemsCount = uiState.armActions.size
                        if (itemsCount == 0) {
                            Text("暂无动作数据", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(itemsCount) { index ->
                                    val arm = uiState.armActions[index]
                                    val isSelected = currentActionId == arm.arm_id

                                    Box(
                                        modifier = Modifier
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0x4D007AFF) else Color(0x33FFFFFF))
                                            .border(1.dp, if (isSelected) Color(0xFF007AFF) else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { onSelect(arm) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(arm.arm_name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
                }
            }
        }
    }
}

// ==========================================
// 6. 音色选择弹窗复用
// ==========================================

@Composable
fun SynthesisVoiceSelectionDialog(
    currentVoice: VoiceBean,
    onSelect: (VoiceBean) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelectedVoice by remember { mutableStateOf(currentVoice) }

    val categories = listOf("全部", "火山", "讯飞", "微软", "中文", "日文", "英文", "方言")
    var selectedCategory by remember { mutableStateOf("全部") }
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 40

    val filteredList = remember(selectedCategory, searchQuery) {
        val allVoices = com.example.guangnian.feature.console.data.bean.StaticVoiceData.allVoices
        allVoices.filter { voice ->
            val lang = voice.language
            val sourceStr = voice.source.toString()
            val categoryMatch = when (selectedCategory) {
                "火山" -> sourceStr.contains("VOLCANO")
                "讯飞" -> sourceStr.contains("AIUI")
                "微软" -> sourceStr.contains("MICROSOFT")
                "中文" -> lang.contains("中") || lang.contains("粤") || lang.contains("台") || lang.contains("吴")
                "日文" -> lang.contains("日")
                "英文" -> lang.contains("英")
                "方言" -> lang.contains("粤") || lang.contains("台") || lang.contains("吴") ||
                        lang.contains("官话") || lang.contains("川") || lang.contains("闽") ||
                        lang.contains("客") || lang.contains("广西")
                else -> true
            }
            val searchMatch = if (searchQuery.isBlank()) true else {
                voice.sex.contains(searchQuery, ignoreCase = true) ||
                lang.contains(searchQuery, ignoreCase = true)
            }
            categoryMatch && searchMatch
        }
    }

    val totalPages = maxOf(1, (filteredList.size + pageSize - 1) / pageSize)
    val pagedList = remember(filteredList, currentPage) { filteredList.drop(currentPage * pageSize).take(pageSize) }

    LaunchedEffect(selectedCategory, searchQuery) { currentPage = 0 }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1E1E1E)) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "《 选择音色",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onDismiss() }
                            .padding(end = 12.dp, top = 2.dp, start = 2.dp)
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .width(260.dp)
                            .height(36.dp)
                            .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                                if (searchQuery.isEmpty()) {
                                    Text("搜索当前分类...", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color(0xFF007AFF) else Color(0x33FFFFFF)).clickable { selectedCategory = category }.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text(category, color = Color.White, fontSize = 12.sp) }
                    }
                }

                if (pagedList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("该分类下暂无音色", color = Color.Gray, fontSize = 12.sp) }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(pagedList) { voice ->
                            val isSelected = voice.id == tempSelectedVoice.id || (tempSelectedVoice.id.isEmpty() && voice.sex == tempSelectedVoice.sex)
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (isSelected) Color(0x4D007AFF) else Color(0x1AFFFFFF)).border(1.dp, if (isSelected) Color(0xFF007AFF) else Color.Transparent, RoundedCornerShape(6.dp)).clickable { tempSelectedVoice = voice }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = { tempSelectedVoice = voice }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF), unselectedColor = Color.LightGray), modifier = Modifier.scale(0.8f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = voice.sex.ifEmpty { "默认" }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "平台: ${voice.source} | 语言: ${voice.language}", color = Color(0xFFAAAAAA), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("上一页", color = if (currentPage > 0) Color(0xFF007AFF) else Color.Gray, fontSize = 12.sp) }
                        Text(text = "${currentPage + 1} / $totalPages", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp)
                        TextButton(onClick = { if (currentPage < totalPages - 1) currentPage++ }, enabled = currentPage < totalPages - 1, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("下一页", color = if (currentPage < totalPages - 1) Color(0xFF007AFF) else Color.Gray, fontSize = 12.sp) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("取消", color = Color.Gray, fontSize = 12.sp) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onSelect(tempSelectedVoice) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007AFF)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(32.dp)) { Text("确定选择", color = Color.White, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}
