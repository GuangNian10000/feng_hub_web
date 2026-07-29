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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.example.guangnian.feature.console.viewmodel.ConsoleViewModel

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle, 
                    color = Color(0xFFAAAAAA), 
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF007AFF),
                    checkedTrackColor = Color(0xFF007AFF).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle, 
                    color = Color(0xFFAAAAAA), 
                    fontSize = 11.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    modifier = Modifier.width(70.dp).height(32.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(10.dp))

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
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("保存", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
