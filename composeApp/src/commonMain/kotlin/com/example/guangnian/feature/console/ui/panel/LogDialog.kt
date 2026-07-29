package com.example.guangnian.feature.console.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.core.utils.LogCollector
import com.example.guangnian.core.utils.LogMessage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun LogDialog(
    onClose: () -> Unit
) {
    val logs by LogCollector.logs.collectAsState()
    val listState = rememberLazyListState()

    var width by remember { mutableStateOf(500.dp) }
    var height by remember { mutableStateOf(250.dp) }
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }
    var isInitialized by remember { mutableStateOf(false) }

    var showD by remember { mutableStateOf(true) }
    var showI by remember { mutableStateOf(true) }
    var showW by remember { mutableStateOf(true) }
    var showE by remember { mutableStateOf(true) }
    var autoScroll by remember { mutableStateOf(true) }

    val filteredLogs = remember(logs, showD, showI, showW, showE) {
        logs.filter {
            (it.level == "D" && showD) ||
            (it.level == "I" && showI) ||
            (it.level == "W" && showW) ||
            (it.level == "E" && showE)
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (!isInitialized) {
            offsetX = (maxWidth - width) / 2
            offsetY = (maxHeight - height) / 2
            isInitialized = true
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
                .size(width, height)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xEE1E1E1E))
                .padding(2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Draggable for moving)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2C))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x.toDp()
                                offsetY += dragAmount.y.toDp()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Text("系统实时日志", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FilterCheckbox("滚动", Color.White, autoScroll) { autoScroll = it }

                        // Filters
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterCheckbox("D", Color(0xFFB0BEC5), showD) { showD = it }
                            FilterCheckbox("I", Color(0xFF4CAF50), showI) { showI = it }
                            FilterCheckbox("W", Color(0xFFFFA000), showW) { showW = it }
                            FilterCheckbox("E", Color(0xFFE53935), showE) { showE = it }
                        }
                        
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color(0xFFD14343),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { LogCollector.clear() }
                        )

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onClose() }
                        )
                    }
                }
                
                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color(0x33000000))
                ) {
                    if (filteredLogs.isEmpty()) {
                        Text("暂无日志", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredLogs) { log ->
                                LogItemRow(log)
                            }
                        }
                    }
                }
            }

            // Resizing handles (four corners)
            val handleSize = 24.dp
            
            // Top-Left
            Box(modifier = Modifier
                .size(handleSize)
                .align(Alignment.TopStart)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x.toDp()
                        val dy = dragAmount.y.toDp()
                        val newWidth = (width - dx).coerceAtLeast(300.dp)
                        val newHeight = (height - dy).coerceAtLeast(200.dp)
                        if (newWidth > 300.dp) offsetX += dx
                        if (newHeight > 200.dp) offsetY += dy
                        width = newWidth
                        height = newHeight
                    }
                }
            )
            // Top-Right
            Box(modifier = Modifier
                .size(handleSize)
                .align(Alignment.TopEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x.toDp()
                        val dy = dragAmount.y.toDp()
                        val newHeight = (height - dy).coerceAtLeast(200.dp)
                        if (newHeight > 200.dp) offsetY += dy
                        width = (width + dx).coerceAtLeast(300.dp)
                        height = newHeight
                    }
                }
            )
            // Bottom-Left
            Box(modifier = Modifier
                .size(handleSize)
                .align(Alignment.BottomStart)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x.toDp()
                        val dy = dragAmount.y.toDp()
                        val newWidth = (width - dx).coerceAtLeast(300.dp)
                        if (newWidth > 300.dp) offsetX += dx
                        width = newWidth
                        height = (height + dy).coerceAtLeast(200.dp)
                    }
                }
            )
            // Bottom-Right
            Box(modifier = Modifier
                .size(handleSize)
                .align(Alignment.BottomEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        width = (width + dragAmount.x.toDp()).coerceAtLeast(300.dp)
                        height = (height + dragAmount.y.toDp()).coerceAtLeast(200.dp)
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterCheckbox(label: String, color: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = Color.Gray),
            modifier = Modifier.size(20.dp).padding(2.dp)
        )
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
    }
}

@Composable
private fun LogItemRow(log: LogMessage) {
    val clipboardManager = LocalClipboardManager.current
    val color = when (log.level) {
        "E" -> Color(0xFFE53935)
        "W" -> Color(0xFFFFA000)
        "I" -> Color(0xFF4CAF50)
        else -> Color(0xFFB0BEC5)
    }

    val timeString = remember(log.timestamp) {
        val dt = Instant.fromEpochMilliseconds(log.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        val h = dt.hour.toString().padStart(2, '0')
        val m = dt.minute.toString().padStart(2, '0')
        val s = dt.second.toString().padStart(2, '0')
        val ms = (dt.nanosecond / 1000000).toString().padStart(3, '0')
        "$h:$m:$s.$ms"
    }

    val fullLogString = "$timeString ${log.level} [${log.tag}] ${log.message}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipboardManager.setText(AnnotatedString(fullLogString))
            }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "$timeString ", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = "${log.level} ", color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(text = "[${log.tag}] ", color = Color(0xFF81D4FA), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = log.message, color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
