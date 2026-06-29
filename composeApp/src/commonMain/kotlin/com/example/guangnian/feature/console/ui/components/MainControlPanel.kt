package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 优化的槽位式主控制面板
 * 提供了统一的头部导航、Tab 切换逻辑和右侧操作按钮
 */
@Composable
fun MainControlPanel(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<String> = emptyList(),
    selectedTab: String = "",
    onTabChange: (String) -> Unit = {},
    actionButton: @Composable (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassBox(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        bgColor = Color(0xD9121212)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // 头部栏
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回/关闭按钮
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (tabs.isEmpty()) {
                    // 无 Tab 模式：显示标题
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // 有 Tab 模式：显示可点击的 Tab 列表
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = tab == selectedTab
                            Surface(
                                onClick = { onTabChange(tab) },
                                color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            if (index < tabs.size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // 右侧动作按钮槽位
                actionButton?.invoke()
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 内容槽位
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                content = content
            )
        }
    }
}

/**
 * 预设的添加按钮组件，方便在 actionButton 槽位中使用
 */
@Composable
fun PanelAddButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
