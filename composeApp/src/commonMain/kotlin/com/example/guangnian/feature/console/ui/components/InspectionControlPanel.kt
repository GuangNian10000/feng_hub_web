package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.guangnian.core.designsystem.components.GlassBox
import com.example.guangnian.core.designsystem.components.ControlButton

@Composable
fun InspectionControlPanel(
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier
            .width(120.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(6.dp)
        ) {
            val pauseBtnText = if (isPaused) "继续巡检" else "暂停巡检"
            val pauseBtnColor = if (isPaused) Color(0xFF388E3C) else Color(0xFFC2185B)

            ControlButton(
                text = pauseBtnText,
                bgColor = pauseBtnColor,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                onClick = onPauseToggle
            )

            ControlButton(
                text = "取消巡检",
                bgColor = Color(0xFF616161),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                onClick = onCancelClick
            )
        }
    }
}
