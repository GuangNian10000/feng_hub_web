package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.guangnian.core.designsystem.components.ControlButton
import com.example.guangnian.core.designsystem.components.GlassBox
import com.example.guangnian.feature.console.data.bean.AgentStatusBean

@Composable
fun AgentControlPanel(
    status: AgentStatusBean?,
    onWakeUpToggle: () -> Unit,
    onAsrToggle: () -> Unit,
    onInterruptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasStatus = status != null

    GlassBox(modifier = modifier.width(120.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(6.dp)
        ) {
            ControlButton(
                text = if (status?.isWakeUp != true) "休眠" else "唤醒",
                bgColor = if (status?.isWakeUp != true) Color(0xFFC2185B) else Color(0xFF388E3C),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                enabled = hasStatus,
                onClick = onWakeUpToggle
            )

            ControlButton(
                text = if (status?.isAsrEnabled != true) "关麦" else "开麦",
                bgColor = if (status?.isAsrEnabled != true) Color(0xFFC2185B) else Color(0xFF388E3C),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                enabled = hasStatus,
                onClick = onAsrToggle
            )

            ControlButton(
                text = "打断",
                bgColor = Color(0xFF1565C0),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                onClick = onInterruptClick
            )
        }
    }
}
