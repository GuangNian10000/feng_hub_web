package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ComposeJoystick(
    modifier: Modifier = Modifier,
    onMove: (x: Float, y: Float, isDragging: Boolean) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .background(Color(0x33000000), CircleShape)
            .border(1.dp, Color(0x22FFFFFF), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f, false)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f, false)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val newX = thumbOffset.x + dragAmount.x
                    val newY = thumbOffset.y + dragAmount.y
                    val distance = hypot(newX, newY)
                    thumbOffset = if (distance <= radius) Offset(newX, newY) else {
                        val angle = atan2(newY, newX)
                        Offset(cos(angle) * radius, sin(angle) * radius)
                    }
                    onMove(thumbOffset.x / radius, -thumbOffset.y / radius, true)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            radius = size.width / 2f
            drawCircle(color = Color(0xBBFFFFFF), radius = radius * 0.35f, center = center + thumbOffset)
        }
    }
}
