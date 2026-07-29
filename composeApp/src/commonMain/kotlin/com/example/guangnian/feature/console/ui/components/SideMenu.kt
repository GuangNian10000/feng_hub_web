package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.core.designsystem.components.GlassBox

@Composable
fun SideMenu(
    menus: List<String>,
    activeFragment: String?,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // bgColor 改为半透明，同时保持内边距一致
    GlassBox(
        modifier = modifier.heightIn(max = 300.dp), 
        bgColor = Color(0xCC1A1A1A) 
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp), 
            contentPadding = PaddingValues(16.dp), // 统一四周内边距
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            items(menus) { menu ->
                val icon = when (menu) {
                    "姿态动作" -> Icons.Default.Accessibility
                    "动作" -> Icons.AutoMirrored.Filled.DirectionsRun
                    "动作编辑" -> Icons.Default.Edit
                    "导览管理" -> Icons.Default.Explore
                    "语音合成" -> Icons.Default.RecordVoiceOver
                    "智能体设置" -> Icons.Default.SmartToy
                    "地图管理" -> Icons.Default.Map
                    "系统设置" -> Icons.Default.Settings
                    "知识库" -> Icons.AutoMirrored.Filled.MenuBook
                    else -> Icons.Default.MoreHoriz
                }
                GridMenuItem(
                    text = menu,
                    icon = icon,
                    isSelected = activeFragment == menu,
                    onClick = { onMenuClick(menu) }
                )
            }
        }
    }
}

@Composable
fun GridMenuItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0x33FFFFFF) else Color(0x11FFFFFF))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp), // 缩小垂直内边距使 item 变小
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isSelected) Color.White else Color.LightGray,
            modifier = Modifier.size(22.dp) // 图标从 28dp 缩小到 22dp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.LightGray,
            fontSize = 11.sp, // 文字从 12sp 缩小到 11sp
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
