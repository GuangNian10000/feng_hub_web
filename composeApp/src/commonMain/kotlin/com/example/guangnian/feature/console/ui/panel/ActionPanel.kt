package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.feature.console.data.bean.ArmActionBean
import com.example.guangnian.feature.console.data.bean.FavoriteActionBean

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionPanel(
    armActions: List<ArmActionBean>,
    favoriteActions: List<FavoriteActionBean>,
    onActionClick: (String, String) -> Unit,
    onActionLongClick: (String, String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (armActions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无动作数据", color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x0D000000))
                        .clickable { onRetryClick() }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("点击重试", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(armActions) { armAction ->
                val isFavorited = favoriteActions.any { it.id == armAction.arm_id && it.isArmAction }
                val displayName = if (isFavorited) "★ ${armAction.arm_name}" else armAction.arm_name
                val subText = if (armAction.time > 0) "ID: ${armAction.arm_id} (${armAction.time.toInt()}s)" else "ID: ${armAction.arm_id}"

                ActionItem(
                    title = displayName,
                    subtitle = subText,
                    isFavorited = isFavorited,
                    onClick = { onActionClick(armAction.arm_id, armAction.arm_name) },
                    onLongClick = { onActionLongClick(armAction.arm_id, armAction.arm_name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionItem(
    title: String,
    subtitle: String,
    isFavorited: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x33FFFFFF))
            .border(0.5.dp, if (isFavorited) Color(0xFFFFD700) else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = if (isFavorited) Color(0xFFFFD700) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color(0x99FFFFFF),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}