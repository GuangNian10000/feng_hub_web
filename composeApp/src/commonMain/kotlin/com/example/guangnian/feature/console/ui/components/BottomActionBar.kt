package com.example.guangnian.feature.console.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.core.designsystem.components.GlassBox
import com.example.guangnian.feature.console.data.bean.FavoriteActionBean

@Composable
fun BottomActionBar(
    isMenuOpen: Boolean,
    onMenuToggle: () -> Unit,
    isPosturesExpanded: Boolean,
    onExpandToggle: () -> Unit,
    favoriteActions: List<FavoriteActionBean>,
    onActionClick: (FavoriteActionBean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        GlassBox(
            modifier = Modifier
                .wrapContentWidth()
                .height(56.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bgColor by animateColorAsState(
                    targetValue = if (isMenuOpen) Color(0x66007AFF) else Color(0x33FFFFFF),
                    animationSpec = tween(300),
                    label = "MenuBgColor"
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onMenuToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isMenuOpen,
                        transitionSpec = {
                            (scaleIn(tween(200)) + fadeIn(tween(200))).togetherWith(scaleOut(tween(200)) + fadeOut(tween(200)))
                        },
                        label = "MenuIconAnim"
                    ) { open ->
                        Text(
                            text = if (open) "✕" else "⊞",
                            color = Color.White,
                            fontSize = if (open) 16.sp else 18.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f, fill = false),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isPosturesExpanded,
                        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (favoriteActions.isEmpty()) {
                                Text(
                                    text = "暂无收藏动作，请在菜单中长按添加",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            } else {
                                favoriteActions.forEach { fav ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onActionClick(fav) }
                                            .padding(horizontal = 10.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = fav.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onExpandToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = isPosturesExpanded, label = "ExpandIconAnim") { expanded ->
                        Text(text = if (expanded) "❯" else "❮", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
