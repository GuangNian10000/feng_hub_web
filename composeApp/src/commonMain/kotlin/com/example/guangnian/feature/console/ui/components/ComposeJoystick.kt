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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class JoystickLevelMode {
    Radial,
    Horizontal
}

internal fun joystickHapticBand(level: Int): Int = when {
    level >= 9 -> 3
    level >= 6 -> 2
    level >= 3 -> 1
    else -> 0
}

@Composable
fun ComposeJoystick(
    modifier: Modifier = Modifier,
    levelMode: JoystickLevelMode = JoystickLevelMode.Radial,
    onMove: (x: Float, y: Float, isDragging: Boolean) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableFloatStateOf(0f) }
    var lastHapticBand by remember { mutableIntStateOf(0) }

    fun resetJoystick() {
        thumbOffset = Offset.Zero
        lastHapticBand = 0
        onMove(0f, 0f, false)
    }

    Box(
        modifier = modifier
            .background(Color(0x33000000), CircleShape)
            .border(1.dp, Color(0x22FFFFFF), CircleShape)
            .pointerInput(levelMode) {
                detectDragGestures(
                    onDragEnd = ::resetJoystick,
                    onDragCancel = ::resetJoystick
                ) { change, dragAmount ->
                    change.consume()
                    val newX = thumbOffset.x + dragAmount.x
                    val newY = thumbOffset.y + dragAmount.y
                    val distance = hypot(newX, newY)
                    thumbOffset = if (distance <= radius) Offset(newX, newY) else {
                        val angle = atan2(newY, newX)
                        Offset(cos(angle) * radius, sin(angle) * radius)
                    }
                    val normalizedX = thumbOffset.x / radius
                    val normalizedY = -thumbOffset.y / radius
                    val magnitude = when (levelMode) {
                        JoystickLevelMode.Radial -> hypot(normalizedX, normalizedY)
                        JoystickLevelMode.Horizontal -> abs(normalizedX)
                    }
                    val hapticBand = joystickHapticBand((magnitude * 10).toInt().coerceIn(0, 10))
                    if (hapticBand != lastHapticBand) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastHapticBand = hapticBand
                    }
                    onMove(normalizedX, normalizedY, true)
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
