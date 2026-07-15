package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.feature.console.data.bean.MapBean

@Composable
fun MapManagementPanel(
    maps: List<MapBean>,
    onDeleteMap: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf<MapBean?>(null) }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除", color = Color.Black, fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除地图 “${showDeleteConfirm?.map_name}” 吗？", color = Color.DarkGray) },
            containerColor = Color.White,
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(maps) { map ->
                    MapItem(
                        map = map,
                        onDeleteClick = { showDeleteConfirm = map }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapItem(
    map: MapBean,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x12FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = map.map_name.ifEmpty { "未命名地图" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (map.is_active) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("当前使用", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                text = "UID: ${map.map_uid}",
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            map.created_at?.let {
                Text(
                    text = "创建于: $it",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = if (map.is_active) Color.Gray.copy(alpha = 0.5f) else Color(0xFFFF5252),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
