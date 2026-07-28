package com.example.guangnian.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guangnian.core.designsystem.components.GlassBox
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.utils.PgmUtils
import com.example.guangnian.feature.console.data.bean.MapBean
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

enum class EditTool {
    BRUSH, LINE, RECTANGLE, DRAG
}

sealed class DrawAction {
    data class Brush(val path: Path, val color: Color, val strokeWidth: Float) : DrawAction()
    data class Line(val start: Offset, val end: Offset, val color: Color, val strokeWidth: Float) : DrawAction()
    data class Rectangle(val rect: Rect, val color: Color, val strokeWidth: Float) : DrawAction()
}

@Composable
fun MapEditScreen(
    mapBean: MapBean,
    onBack: () -> Unit
) {
    val httpClient = koinInject<HttpClient>()

    var mapBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var loadingStage by remember { mutableStateOf("加载中...") }

    var currentTool by remember { mutableStateOf(EditTool.DRAG) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    
    val history = remember { mutableStateListOf<DrawAction>() }
    
    // Transform state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Current drawing state
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var startOffset by remember { mutableStateOf(Offset.Zero) }
    var endOffset by remember { mutableStateOf(Offset.Zero) }
    var isDrawing by remember { mutableStateOf(false) }

    var showExitConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val saveMap = {
        mapBitmap?.let { bmp ->
            coroutineScope.launch {
                try {
                    isLoading = true
                    loadingStage = "渲染地图..."
                    loadingProgress = 0.2f
                    
                    val resultBitmap = ImageBitmap(bmp.width, bmp.height)
                    val composeCanvas = androidx.compose.ui.graphics.Canvas(resultBitmap)
                    
                    val drawScope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
                    drawScope.draw(
                        density = androidx.compose.ui.unit.Density(1f),
                        layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
                        canvas = composeCanvas,
                        size = androidx.compose.ui.geometry.Size(bmp.width.toFloat(), bmp.height.toFloat())
                    ) {
                        drawImage(bmp)
                        
                        history.forEach { action ->
                            when (action) {
                                is DrawAction.Brush -> drawPath(action.path, action.color, style = androidx.compose.ui.graphics.drawscope.Stroke(action.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                is DrawAction.Line -> drawLine(action.color, action.start, action.end, strokeWidth = action.strokeWidth, cap = StrokeCap.Round)
                                is DrawAction.Rectangle -> drawRect(action.color, action.rect.topLeft, action.rect.size, style = androidx.compose.ui.graphics.drawscope.Stroke(action.strokeWidth))
                            }
                        }
                    }
                    
                    loadingStage = "编码地图..."
                    loadingProgress = 0.5f
                    val pgmBytes = withContext(Dispatchers.Default) {
                        com.example.guangnian.core.utils.encodeImageBitmapToPgm(resultBitmap)
                    }
                    
                    // 导出到本地 Documents 目录，供用户排查线宽问题
                    PgmUtils.exportPgmToDocuments("map_edit_export_${mapBean.map_uid}.pgm", pgmBytes)
                    
                    loadingStage = "上传地图..."
                    loadingProgress = 0.8f
                    val parsedUrl = io.ktor.http.Url(mapBean.image_url)
                    val uploadUrl = "${parsedUrl.protocol.name}://${parsedUrl.host}:${parsedUrl.port}/navigation/maps/${mapBean.map_uid}/pgm"
                    
                    val response = httpClient.put(uploadUrl) {
                        contentType(ContentType("image", "x-portable-graymap"))
                        setBody(pgmBytes)
                    }
                    
                    if (response.status.value in 200..299) {
                        KLog.i("MapEditScreen", "保存地图成功: $uploadUrl")
                    } else {
                        KLog.e("MapEditScreen", "保存地图失败: HTTP ${response.status}")
                    }
                } catch (e: Exception) {
                    KLog.e("MapEditScreen", "保存地图异常: ${e.message}", e)
                } finally {
                    isLoading = false
                    history.clear() // Clear history so exit confirm won't show
                }
            }
        }
    }

    LaunchedEffect(mapBean.image_url) {
        if (mapBean.image_url.isNotEmpty()) {
            try {
                isLoading = true
                loadingStage = "下载地图..."
                val response = httpClient.get(mapBean.image_url)
                val bytes = response.readBytes()
                loadingStage = "解析地图..."
                loadingProgress = 0.8f
                val bitmap = withContext(Dispatchers.Default) {
                    PgmUtils.decodePgmToBitmap(bytes)
                }
                mapBitmap = bitmap
                loadingProgress = 1.0f
                isLoading = false
            } catch (e: Exception) {
                KLog.e("MapEditScreen", "Failed to load map: ${e.message}")
                isLoading = false
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("确认退出", fontWeight = FontWeight.Bold) },
            text = { Text("退出前是否保存修改？") },
            confirmButton = {
                TextButton(onClick = { 
                    saveMap()
                    showExitConfirm = false
                    onBack()
                }) {
                    Text("保存并退出", color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExitConfirm = false
                    onBack()
                }) {
                    Text("直接退出", color = Color.Gray)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // Map Canvas Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentTool) {
                    if (currentTool == EditTool.DRAG) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.1f, 10f)
                            offset += pan
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { start ->
                                isDrawing = true
                                // Adjust start point based on scale and offset
                                val adjustedStart = (start - offset) / scale
                                startOffset = adjustedStart
                                endOffset = adjustedStart
                                if (currentTool == EditTool.BRUSH) {
                                    currentPath = Path().apply { moveTo(adjustedStart.x, adjustedStart.y) }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val adjustedChange = dragAmount / scale
                                endOffset += adjustedChange
                                if (currentTool == EditTool.BRUSH) {
                                    currentPath?.lineTo(endOffset.x, endOffset.y)
                                    // Trigger recomposition
                                    val p = currentPath
                                    currentPath = null
                                    currentPath = p
                                }
                            },
                            onDragEnd = {
                                when (currentTool) {
                                    EditTool.BRUSH -> {
                                        currentPath?.let { history.add(DrawAction.Brush(it, selectedColor, strokeWidth)) }
                                    }
                                    EditTool.LINE -> {
                                        history.add(DrawAction.Line(startOffset, endOffset, selectedColor, strokeWidth))
                                    }
                                    EditTool.RECTANGLE -> {
                                        history.add(DrawAction.Rectangle(Rect(startOffset, endOffset), selectedColor, strokeWidth))
                                    }
                                    else -> {}
                                }
                                isDrawing = false
                                currentPath = null
                            }
                        )
                    }
                }
        ) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            // Initial auto-center and scale
            LaunchedEffect(mapBitmap) {
                mapBitmap?.let { bmp ->
                    val scaleW = canvasWidth / bmp.width
                    val scaleH = canvasHeight / bmp.height
                    scale = minOf(scaleW, scaleH) * 0.8f
                    offset = Offset(
                        (canvasWidth - bmp.width * scale) / 2f,
                        (canvasHeight - bmp.height * scale) / 2f
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                translationX = offset.x
                translationY = offset.y
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }) {
                mapBitmap?.let {
                    drawImage(it)
                }

                // Draw History
                history.forEach { action ->
                    when (action) {
                        is DrawAction.Brush -> drawPath(action.path, action.color, style = Stroke(action.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        is DrawAction.Line -> drawLine(action.color, action.start, action.end, strokeWidth = action.strokeWidth, cap = StrokeCap.Round)
                        is DrawAction.Rectangle -> drawRect(action.color, action.rect.topLeft, action.rect.size, style = Stroke(action.strokeWidth))
                    }
                }

                // Draw Current Action
                if (isDrawing) {
                    when (currentTool) {
                        EditTool.BRUSH -> currentPath?.let { drawPath(it, selectedColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                        EditTool.LINE -> drawLine(selectedColor, startOffset, endOffset, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                        EditTool.RECTANGLE -> drawRect(selectedColor, Rect(startOffset, endOffset).topLeft, Rect(startOffset, endOffset).size, style = Stroke(strokeWidth))
                        else -> {}
                    }
                }
            }
        }

        // Top Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (history.isNotEmpty()) {
                        showExitConfirm = true
                    } else {
                        onBack()
                    }
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                }

                Text("地图编辑: ${mapBean.map_name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Button(
                    onClick = { saveMap() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("保存", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Side Toolbar (Right)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            GlassBox(bgColor = Color(0xCC1E1E1E)) {
                Column(modifier = Modifier.padding(4.dp)) {
                    ToolItem(Icons.Default.Brush, "画笔", currentTool == EditTool.BRUSH) { currentTool = EditTool.BRUSH }
                    ToolItem(Icons.Default.Timeline, "线段", currentTool == EditTool.LINE) { currentTool = EditTool.LINE }
                    ToolItem(Icons.Default.CheckBoxOutlineBlank, "矩形", currentTool == EditTool.RECTANGLE) { currentTool = EditTool.RECTANGLE }
                    ToolItem(Icons.Default.PanTool, "平移", currentTool == EditTool.DRAG) { currentTool = EditTool.DRAG }
                }
            }
        }

        // Bottom Settings Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            GlassBox(bgColor = Color(0xCC1E1E1E)) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color Selection
                    ColorOption(Color.Black, selectedColor == Color.Black) { selectedColor = Color.Black }
                    Spacer(modifier = Modifier.width(8.dp))
                    ColorOption(Color.White, selectedColor == Color.White) { selectedColor = Color.White }

                    Spacer(modifier = Modifier.width(12.dp))
                    Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.width(12.dp))

                    // Thickness Slider
                    Icon(Icons.Default.LineWeight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 1f..200f,
                        modifier = Modifier.width(120.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF007AFF))
                    )
                    Text("${strokeWidth.toInt()} px", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(40.dp))

                    Spacer(modifier = Modifier.width(8.dp))
                    Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.2f))

                    // Undo
                    IconButton(onClick = { if (history.isNotEmpty()) history.removeAt(history.size - 1) }, enabled = history.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销", tint = if (history.isNotEmpty()) Color.White else Color.Gray)
                    }
                }
            }
        }

        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(progress = loadingProgress, color = Color(0xFF007AFF))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadingStage, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ColorOption(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF007AFF) else Color.Gray, CircleShape)
            .clickable { onClick() }
    )
}

@Composable
fun ToolItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF007AFF) else Color.Transparent)
            .clickable { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
