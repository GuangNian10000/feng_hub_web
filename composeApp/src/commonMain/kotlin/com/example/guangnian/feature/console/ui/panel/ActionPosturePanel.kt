package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.feature.console.data.bean.FavoriteActionBean
import com.example.guangnian.feature.console.data.bean.PostureBean

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionPosturePanel(
    postures: List<PostureBean>,
    favoriteActions: List<FavoriteActionBean>,
    onActionClick: (String, String) -> Unit,
    onActionLongClick: (String, String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (postures.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "暂无动作数据，点击重试",
                color = Color(0xFF007AFF),
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x0D000000))
                    .clickable { onRetryClick() }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(postures) { posture ->
                val isFavorited = favoriteActions.any { it.id == posture.posture_name && !it.isArmAction }
                val displayName = if (isFavorited) "★ ${posture.name}" else posture.name

                ActionItem(
                    text = displayName,
                    isFavorited = isFavorited,
                    onClick = { onActionClick(posture.posture_name, posture.name) },
                    onLongClick = { onActionLongClick(posture.posture_name, posture.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionItem(text: String, isFavorited: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x33FFFFFF))
            .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isFavorited) Color(0xFFFFD700) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}