package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.utils.PgmUtils
import com.example.guangnian.feature.console.data.bean.MapBean
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import org.koin.compose.koinInject

@Composable
fun MapManagementPanel(
    maps: List<MapBean>,
    onDeleteMap: (String) -> Unit,
    onSwitchMap: (String) -> Unit,
    onEditMap: (MapBean) -> Unit,
    onStartMapping: (String) -> Unit,
    onSetAddAction: (String, () -> Unit) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf<MapBean?>(null) }
    var showSwitchConfirm by remember { mutableStateOf<MapBean?>(null) }
    var showStartMappingDialog by remember { mutableStateOf(false) }
    var newMapName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onSetAddAction("开始建图") {
            newMapName = ""
            showStartMappingDialog = true
        }
    }

    if (showStartMappingDialog) {
        AlertDialog(
            onDismissRequest = { showStartMappingDialog = false },
            title = { Text("开始建图", color = Color.Black, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("请输入新地图的名称：", color = Color.DarkGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMapName,
                        onValueChange = { newMapName = it },
                        label = { Text("地图名称") },
                        placeholder = { Text("例如：办公室_1F") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color(0xFFF7F9FC),
                            textColor = Color(0xFF1F2937),
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFB8C2D1),
                            focusedLabelColor = Color(0xFF007AFF),
                            unfocusedLabelColor = Color(0xFF5F6B7A),
                            placeholderColor = Color(0xFF8A95A5),
                            cursorColor = Color(0xFF007AFF)
                        )
                    )
                }
            },
            backgroundColor = Color.White,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMapName.isNotBlank()) {
                            onStartMapping(newMapName)
                            showStartMappingDialog = false
                        }
                    },
                    enabled = newMapName.isNotBlank()
                ) {
                    Text("开始", color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartMappingDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除", color = Color.Black, fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除地图 “${showDeleteConfirm?.map_name}” 吗？", color = Color.DarkGray) },
            backgroundColor = Color.White,
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm?.let { onDeleteMap(it.map_uid) }
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    if (showSwitchConfirm != null) {
        AlertDialog(
            onDismissRequest = { showSwitchConfirm = null },
            title = { Text("确认切换地图", color = Color.Black, fontWeight = FontWeight.Bold) },
            text = { Text("确定要切换到地图 “${showSwitchConfirm?.map_name}” 吗？", color = Color.DarkGray) },
            backgroundColor = Color.White,
            confirmButton = {
                TextButton(onClick = {
                    showSwitchConfirm?.let { onSwitchMap(it.map_uid) }
                    showSwitchConfirm = null
                }) {
                    Text("切换", color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchConfirm = null }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(4.dp)) {
        if (maps.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无地图数据", color = Color.Gray, fontSize = 14.sp)
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(maps) { map ->
                    MapItem(
                        map = map,
                        onDeleteClick = { showDeleteConfirm = map },
                        onEditClick = { onEditMap(map) },
                        onClick = {
                            if (!map.is_active) {
                                showSwitchConfirm = map
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapItem(
    map: MapBean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onClick: () -> Unit
) {
    val httpClient = koinInject<HttpClient>()
    var imageBitmap by remember(map.image_url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(map.image_url) {
        if (map.image_url.isNotEmpty()) {
            try {
                val bytes = httpClient.get(map.image_url).readBytes()
                imageBitmap = PgmUtils.decodePgmToBitmap(bytes)
            } catch (e: Exception) {
                KLog.w("MapManagementPanel", "加载地图预览失败: ${e.message}", e)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x1AFFFFFF))
            .border(
                width = if (map.is_active) 2.dp else 0.5.dp,
                color = if (map.is_active) Color(0xFF007AFF) else Color(0x12FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 地图预览图 (手动解析 PGM)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = map.map_name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = map.map_name.ifEmpty { "未命名地图" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (map.is_active) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF007AFF))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("正在使用", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "编辑",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = if (map.is_active) Color.Gray.copy(alpha = 0.5f) else Color(0xFFFF5252),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
