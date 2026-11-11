package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.guangnian.feature.console.data.bean.*
import com.example.guangnian.feature.console.viewmodel.AgentViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.koin.compose.viewmodel.koinViewModel

/**
 * 智能体设置面板（手机控制端）
 * 数据结构与平板端 (feng_voice) 全面统一：
 * - 智能体实体：VoiceAgent (id, prompt, vcn, voiceName, engine, isPreset 等)
 * - 音色库：VoiceCatalog (包含国内基础版 59 款与国际版 118 款发音人)
 * - 提示词预设：VoiceConstants.PROMPT_PRESETS & INTL_PROMPT_PRESETS
 * - 支持手机端直接对平板端智能体执行查询、切换(选中)、新增、修改与删除
 */
@Composable
fun AgentSettingsPanel(
    onSetAddAction: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    agentViewModel: AgentViewModel = koinViewModel()
) {
    val uiState by agentViewModel.uiState.collectAsState()
    val agentData = uiState.agentData
    val isLoading = uiState.isLoading

    var showDeleteConfirm by remember { mutableStateOf<VoiceAgent?>(null) }
    var showSelectConfirm by remember { mutableStateOf<VoiceAgent?>(null) }
    var showEditDialog by remember { mutableStateOf<VoiceAgent?>(null) }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isLoading) {
                if (agentData.list.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无智能体数据，点击右上角新增", color = Color.LightGray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(agentData.list, key = { it.id.ifEmpty { it.vcn } }) { agent ->
                            val isSelected = agent.id == agentData.agent.id
                            AgentCardItem(
                                agent = agent,
                                isSelected = isSelected,
                                onSelect = {
                                    if (!isSelected) showSelectConfirm = agent
                                },
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
                CircularProgressIndicator(color = Color(0xFF00E5FF))
            }
        }

        // 跨平台 Toast UI 层
        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xD9000000))
                    .border(0.8.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = toastMessage!!, color = Color.White, fontSize = 13.sp)
            }
        }

        // 切换确认弹窗
        showSelectConfirm?.let { agent ->
            ConfirmDialog(
                title = "应用智能体",
                content = "确定要启用智能体【${agent.voiceName.ifEmpty { agent.name }}】吗？平板端将同步切换音色与人设。",
                onConfirm = {
                    agentViewModel.selectAgent(agent)
                    showSelectConfirm = null
                },
                onDismiss = { showSelectConfirm = null }
            )
        }

        // 删除确认弹窗
        showDeleteConfirm?.let { agent ->
            ConfirmDialog(
                title = "删除智能体",
                content = "确定要删除智能体【${agent.voiceName.ifEmpty { agent.name }}】吗？此操作不可撤销。",
                onConfirm = {
                    agentViewModel.deleteAgent(agent)
                    showDeleteConfirm = null
                },
                onDismiss = { showDeleteConfirm = null }
            )
        }

        // 编辑弹窗
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

        // 新增弹窗
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

/**
 * 智能体卡片单项
 */
@Composable
private fun AgentCardItem(
    agent: VoiceAgent,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIntl = agent.engine.equals("INTERNATIONAL", ignoreCase = true)
    val borderColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
    val bgColor = if (isSelected) Color(0x1F0284C7) else Color(0xFF0F172A)

    Card(
        backgroundColor = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.2.dp else 0.8.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 第一行：单选指示器 + 引擎标签 + 预设标签 + 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelect,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF00E5FF),
                            unselectedColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.size(18.dp)
                    )

                    // 运行中状态标签
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .border(0.6.dp, Color(0xFF10B981), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("已启用", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 引擎标签
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isIntl) Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                else Color(0xFF0284C7).copy(alpha = 0.2f)
                            )
                            .border(
                                0.6.dp,
                                if (isIntl) Color(0xFFA78BFA) else Color(0xFF38BDF8),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isIntl) "国际版 (多语种)" else "基础版 (超拟人)",
                            color = if (isIntl) Color(0xFFA78BFA) else Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 预设标识
                    if (agent.isPreset) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .border(0.6.dp, Color(0xFFF59E0B), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("预设", color = Color(0xFFFBBF24), fontSize = 8.5.sp)
                        }
                    }
                }

                // 操作按钮区
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "编辑",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onEdit() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "删除",
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // 第二行：发音人音色信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = agent.voiceName.ifEmpty { "默认发音人" },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${agent.vcn})",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp
                )
                if (agent.name.isNotBlank() && agent.name != agent.voiceName) {
                    Text(
                        text = "· ${agent.name}",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.5.sp
                    )
                }
            }

            // 第三行：提示词
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "提示词:",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = agent.prompt.ifBlank { "（无专用人设词，使用默认交互风格）" },
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 确认对话框
 */
@Composable
private fun ConfirmDialog(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        title = {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(content, color = Color(0xFFCBD5E1), fontSize = 12.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("确定", color = Color.White, fontSize = 11.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("取消", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    )
}

/**
 * 新增/编辑智能体弹窗
 */
@Composable
private fun AgentEditDialog(
    initialAgent: VoiceAgent?,
    title: String,
    onConfirm: (VoiceAgent) -> Unit,
    onDismiss: () -> Unit
) {
    val initialIsIntl = initialAgent?.engine?.equals("INTERNATIONAL", ignoreCase = true) ?: false

    var chosenEngine by remember(initialAgent) {
        mutableStateOf(if (initialIsIntl) VoiceEngineType.INTERNATIONAL else VoiceEngineType.BASIC)
    }

    val isBasic = chosenEngine == VoiceEngineType.BASIC
    val targetEngineStr = if (isBasic) "BASIC" else "INTERNATIONAL"

    var name by remember(initialAgent) { mutableStateOf(initialAgent?.name ?: "") }
    var prompt by remember(initialAgent) { mutableStateOf(initialAgent?.prompt ?: "") }
    var vcn by remember(initialAgent) {
        mutableStateOf(initialAgent?.vcn ?: if (isBasic) "x6_lingxiaoyue_pro" else "x4_lingxiaoxuan_em_v2")
    }
    var voiceName by remember(initialAgent) {
        mutableStateOf(initialAgent?.voiceName ?: if (isBasic) "聆小玥" else "聆小璇(多语)")
    }

    var showVoicePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
            modifier = Modifier
                .width(520.dp)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }

                // 1. 所属引擎选择栏 (基础版 vs 国际版)
                Column {
                    Text("所属引擎 (区分基础/国际):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 基础版选择块
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isBasic) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF1E293B))
                                .border(
                                    1.dp,
                                    if (isBasic) Color(0xFF00E5FF) else Color(0xFF334155),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (!isBasic) {
                                        chosenEngine = VoiceEngineType.BASIC
                                        vcn = "x6_lingxiaoyue_pro"
                                        voiceName = "聆小玥"
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "基础版 (国内超拟人)",
                                color = if (isBasic) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (isBasic) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // 国际版选择块
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isBasic) Color(0xFF8B5CF6).copy(alpha = 0.3f) else Color(0xFF1E293B))
                                .border(
                                    1.dp,
                                    if (!isBasic) Color(0xFFA78BFA) else Color(0xFF334155),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (isBasic) {
                                        chosenEngine = VoiceEngineType.INTERNATIONAL
                                        vcn = "x4_lingxiaoxuan_em_v2"
                                        voiceName = "聆小璇(多语)"
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "国际版 (多语种/大模型)",
                                color = if (!isBasic) Color(0xFFA78BFA) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (!isBasic) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 2. 发音人音色选择栏 (点击打开独立音色列表弹窗)
                Column {
                    Text("发音人音色 (点击选择):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(4.dp))
                            .clickable { showVoicePicker = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "$voiceName ($vcn)",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "切换音色",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 3. 人设提示词
                Column {
                    Text("人设提示词 (System Prompt):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    // 预设提示词快捷标签 (根据当前选中的引擎显示国内或国际预设)
                    val presetList = if (isBasic) VoiceConstants.PROMPT_PRESETS else VoiceConstants.INTL_PROMPT_PRESETS
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(presetList) { (tag, tpl) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(0.8.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { prompt = tpl }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(tag, color = Color(0xFF38BDF8), fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = {
                            Text("输入人设背景、性格风格、应答规则等提示词...", color = Color(0xFF64748B), fontSize = 11.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        textStyle = TextStyle(fontSize = 11.sp, color = Color.White, lineHeight = 16.sp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            backgroundColor = Color(0xFF1E293B),
                            cursorColor = Color(0xFF00E5FF)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                }

                // 4. 可选名称
                Column {
                    Text("智能体名称 (可选):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = {
                            Text(voiceName, color = Color(0xFF64748B), fontSize = 11.sp)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            backgroundColor = Color(0xFF1E293B),
                            cursorColor = Color(0xFF00E5FF)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                }

                // 底部操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Text("取消", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val now = Clock.System.now().toEpochMilliseconds()
                            val agentId = initialAgent?.id?.ifEmpty { "agent_$now" } ?: "agent_$now"
                            val resultAgent = (initialAgent ?: VoiceAgent()).copy(
                                id = agentId,
                                name = name.trim().ifEmpty { voiceName },
                                prompt = prompt.trim(),
                                vcn = vcn.trim(),
                                voiceName = voiceName.trim(),
                                engine = targetEngineStr,
                                updatedAt = now
                            )
                            onConfirm(resultAgent)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("保存智能体", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 独立音色选择弹窗
    if (showVoicePicker) {
        VoicePickerDialog(
            engineType = chosenEngine,
            currentVcn = vcn,
            onDismiss = { showVoicePicker = false },
            onSelectVoice = { selectedVoice ->
                val chosenVcn = if (chosenEngine == VoiceEngineType.INTERNATIONAL) {
                    selectedVoice.id.takeIf { it.isNotBlank() } ?: selectedVoice.effectiveVcn
                } else {
                    selectedVoice.effectiveVcn
                }
                vcn = chosenVcn
                voiceName = selectedVoice.name
                showVoicePicker = false
            }
        )
    }
}

/**
 * 独立音色选择列表弹窗
 */
@Composable
private fun VoicePickerDialog(
    engineType: VoiceEngineType,
    currentVcn: String,
    onDismiss: () -> Unit,
    onSelectVoice: (VoiceItem) -> Unit
) {
    val isBasic = engineType == VoiceEngineType.BASIC
    val allVoices = if (isBasic) VoiceCatalog.BASIC_VOICES else VoiceCatalog.INTERNATIONAL_VOICES

    var searchQuery by remember { mutableStateOf("") }
    val filteredVoices = remember(searchQuery, allVoices) {
        if (searchQuery.isBlank()) {
            allVoices
        } else {
            val q = searchQuery.trim()
            allVoices.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.id.contains(q, ignoreCase = true) ||
                it.effectiveVcn.contains(q, ignoreCase = true) ||
                it.language.contains(q, ignoreCase = true) ||
                it.language_label.contains(q, ignoreCase = true) ||
                it.latitude.contains(q, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
            modifier = Modifier
                .width(600.dp)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 头部
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "选择发音人音色 (${allVoices.size} 款 · ${engineType.label})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }

                // 搜索框
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 11.5.sp, color = Color.White),
                    cursorBrush = SolidColor(Color(0xFF00E5FF)),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "搜索发音人姓名、代号或语种 (如 聆小玥、Gavin、英语)...",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "清除", tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                )

                // 音色滚动列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredVoices) { voice ->
                        val isSelected = voice.id == currentVcn || voice.effectiveVcn == currentVcn

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0x330284C7) else Color(0xFF1E293B).copy(alpha = 0.7f))
                                .border(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectVoice(voice) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f, fill = false),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = voice.name,
                                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (voice.pro) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(Color(0xFFEAB308).copy(alpha = 0.2f))
                                                        .border(0.6.dp, Color(0xFFEAB308), RoundedCornerShape(3.dp))
                                                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                ) {
                                                    Text("PRO", color = Color(0xFFFACC15), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            val lang = voice.language_label.ifBlank { voice.language }
                                            if (lang.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(Color(0xFF334155).copy(alpha = 0.8f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(lang, color = Color(0xFFCBD5E1), fontSize = 8.sp)
                                                }
                                            }
                                        }

                                        Text(
                                            text = buildString {
                                                if (!isBasic && voice.id.isNotBlank()) append("ID: ${voice.id} · ")
                                                append(voice.effectiveVcn)
                                                val extra = voice.latitude.ifBlank { voice.tone }
                                                if (extra.isNotBlank()) append(" · $extra")
                                            },
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f)
                                            else Color(0xFF0284C7).copy(alpha = 0.2f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E5FF) else Color(0xFF38BDF8),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onSelectVoice(voice) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isSelected) "已选" else "选用",
                                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF38BDF8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
