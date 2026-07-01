package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.core.designsystem.components.GlassBox
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.console.data.bean.RobotNavState
import com.example.guangnian.feature.console.data.bean.RobotNavStatusBean

@Composable
fun TopStatusBar(
    navStatus: RobotNavStatusBean,
    connectionState: TcpState,
    connectionError: String?,
    latency: Long?,
    onBackClick: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        GlassBox(modifier = Modifier.height(36.dp)) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "〈 返回",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onBackClick() }.padding(end = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.fillMaxHeight(0.5f).width(1.dp), color = Color.Gray)
                Text("BAT 85%", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("TEMP 42°C", color = Color.White, fontSize = 12.sp)
            }
        }

        // 机器人状态区域
        GlassBox(modifier = Modifier.height(36.dp)) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (stateText, stateColor) = when (navStatus.state) {
                    RobotNavState.IDLE -> "空闲" to Color.LightGray
                    RobotNavState.NAV_READY -> "就绪" to Color(0xFF4CAF50)
                    RobotNavState.NAVIGATING -> "导航中" to Color(0xFF2196F3)
                    RobotNavState.RELOCALIZING -> "重定位中" to Color(0xFFFF9800)
                    RobotNavState.EMERGENCY_STOP -> "急停" to Color(0xFFE53935)
                    RobotNavState.MAPPING -> "建图中" to Color(0xFF9C27B0)
                    else -> navStatus.state to Color.White
                }
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(stateColor, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Text(
                    text = "机器人状态: $stateText",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!navStatus.current_point_id.isNullOrBlank()) {
                    Text(
                        text = " -> ${navStatus.current_point_id}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        GlassBox(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onConnectClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (connectionState) {
                    TcpState.CONNECTED -> Color(0xFF4CAF50)
                    TcpState.CONNECTING -> Color(0xFFFFC107)
                    TcpState.DISCONNECTED -> Color(0xFFE53935)
                }
                val statusText = when (connectionState) {
                    TcpState.CONNECTED -> "已连接"
                    TcpState.CONNECTING -> "连接中..."
                    TcpState.DISCONNECTED -> "未连接"
                }

                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                if (connectionState == TcpState.DISCONNECTED && !connectionError.isNullOrEmpty()) {
                    Text(
                        text = connectionError,
                        color = Color(0xFFE53935),
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                } else if (connectionState == TcpState.CONNECTED) {
                    Text("${latency ?: 0} ms", color = Color.LightGray, fontSize = 10.sp)
                } else if (connectionState == TcpState.CONNECTING) {
                    Text("请稍候", color = Color.LightGray, fontSize = 10.sp)
                }
            }
        }
    }
}
