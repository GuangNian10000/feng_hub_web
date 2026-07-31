package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.feature.console.viewmodel.ConsoleViewModel
import com.example.guangnian.core.utils.rememberMediaPicker
import kotlinx.coroutines.delay

@Composable
fun SystemSettingsPanel(
    modifier: Modifier = Modifier,
    consoleViewModel: ConsoleViewModel = koinViewModel()
) {
    val uiState by consoleViewModel.uiState.collectAsState()
    val settingsState = uiState.systemSettingsState

    LaunchedEffect(Unit) {
        consoleViewModel.fetchSystemSettings()
    }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    val mediaPicker = rememberMediaPicker(onMediaPicked = { bytes, mediaType, fileName ->
        consoleViewModel.uploadCarouselMedia(bytes, mediaType, fileName) { success, msg ->
            toastMessage = msg
        }
    })

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 动作分析开关
            SettingSwitchItem(
                title = "动作分析",
                subtitle = "开启后将实时分析机器人的运动状态",
                checked = settingsState.motionAnalysisEnabled,
                onCheckedChange = { consoleViewModel.updateSystemSetting(motionAnalysisEnabled = it) }
            )

            // 打招呼开关
            SettingSwitchItem(
                title = "主动打招呼",
                subtitle = "开启后机器人会在检测到人脸时主动打招呼",
                checked = settingsState.greetEnabled,
                onCheckedChange = { consoleViewModel.updateSystemSetting(greetEnabled = it) }
            )

            // 自动打断开关
            SettingSwitchItem(
                title = "自动打断",
                subtitle = "开启后可在机器人说话时通过语音直接打断",
                checked = settingsState.breakEnabled,
                onCheckedChange = { consoleViewModel.updateSystemSetting(breakEnabled = it) }
            )

            // 打招呼静默时间设置
            SettingInputItem(
                title = "打招呼静默时间 (秒)",
                subtitle = "两次主动打招呼之间的最短间隔时间",
                value = settingsState.greetSilentTime,
                onSave = { time -> consoleViewModel.updateSystemSetting(greetSilentTime = time) }
            )

            // Boot ID 设置
            SettingStringInputItem(
                title = "Boot ID",
                subtitle = "修改当前设备的 Boot ID",
                value = settingsState.bootId,
                onSave = { id -> consoleViewModel.updateSystemSetting(bootId = id) }
            )

            // 打断词设置
            SettingStringInputItem(
                title = "打断词",
                subtitle = "多个词请用逗号分隔",
                value = settingsState.interruptWords.joinToString(", "),
                onSave = { text -> 
                    val words = text.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                    consoleViewModel.updateSystemSetting(interruptWords = words) 
                }
            )

            // 唤醒词设置
            SettingStringInputItem(
                title = "唤醒词",
                subtitle = "多个词请用逗号分隔",
                value = settingsState.wakeUpWords.joinToString(", "),
                onSave = { text -> 
                    val words = text.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                    consoleViewModel.updateSystemSetting(wakeUpWords = words) 
                }
            )

            // 休眠词设置
            SettingStringInputItem(
                title = "休眠词",
                subtitle = "多个词请用逗号分隔",
                value = settingsState.sleepWords.joinToString(", "),
                onSave = { text -> 
                    val words = text.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                    consoleViewModel.updateSystemSetting(sleepWords = words) 
                }
            )

            // 导航词设置
            SettingStringInputItem(
                title = "导航词",
                subtitle = "多个词请用逗号分隔",
                value = settingsState.navigationWords.joinToString(", "),
                onSave = { text -> 
                    val words = text.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                    consoleViewModel.updateSystemSetting(navigationWords = words) 
                }
            )

            // 视觉词设置
            SettingStringInputItem(
                title = "视觉词",
                subtitle = "多个词请用逗号分隔",
                value = settingsState.visionWords.joinToString(", "),
                onSave = { text -> 
                    val words = text.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
                    consoleViewModel.updateSystemSetting(visionWords = words) 
                }
            )

            // 轮播图停留时长设置
            SettingInputItem(
                title = "轮播图停留时长 (秒)",
                subtitle = "每张轮播图片的展示时间",
                value = settingsState.carouselImageDuration,
                onSave = { time -> consoleViewModel.updateSystemSetting(carouselImageDuration = time) }
            )

            // 上传轮播图片或视频
            SettingActionItem(
                title = "上传轮播素材",
                subtitle = "支持图片或视频",
                buttonText = "选择并上传",
                onClick = { mediaPicker.pickMedia() }
            )
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
    }
}

@Composable
private fun SettingActionItem(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle, 
                    color = Color(0xFFAAAAAA), 
                    fontSize = 9.sp
                )
            }
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF007AFF),
                    contentColor = Color.White
                ),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp).height(24.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(buttonText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle, 
                    color = Color(0xFFAAAAAA), 
                    fontSize = 9.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.7f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF007AFF),
                    uncheckedThumbColor = Color(0xFFE0E0E0),
                    uncheckedTrackColor = Color(0x4D000000)
                )
            )
        }
    }
}

@Composable
private fun SettingInputItem(
    title: String,
    subtitle: String,
    value: Int,
    onSave: (Int) -> Unit
) {
    var timeText by remember(value) { mutableStateOf(value.toString()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle, 
                    color = Color(0xFFAAAAAA), 
                    fontSize = 9.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    modifier = Modifier.width(50.dp).height(24.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 11.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(0.5.dp, Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                                .background(Color(0x1A000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        timeText.toIntOrNull()?.let { time ->
                            onSave(time)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp).height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("保存", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SettingStringInputItem(
    title: String,
    subtitle: String,
    value: String,
    onSave: (String) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle, 
                    color = Color(0xFFAAAAAA), 
                    fontSize = 9.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.width(180.dp).height(24.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 11.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(0.5.dp, Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                                .background(Color(0x1A000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        onSave(textValue)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp).height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("保存", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
