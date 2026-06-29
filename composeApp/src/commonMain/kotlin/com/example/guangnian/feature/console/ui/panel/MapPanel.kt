package com.example.guangnian.feature.console.ui.panel

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.bean.*
import com.example.guangnian.core.designsystem.components.GlassBox
import com.example.guangnian.feature.console.utils.MapTransformUtils
import com.example.guangnian.feature.console.viewmodel.MapInteractionMode
import com.example.guangnian.feature.console.viewmodel.MapUiState
import com.example.guangnian.feature.console.viewmodel.MapViewModel
import com.example.guangnian.feature.console.ui.panel.MapUtils.drawDirectionArrow
import com.example.guangnian.feature.console.ui.panel.MapUtils.drawFallbackGrid
import com.example.guangnian.feature.console.ui.panel.MapUtils.drawPathLine
import com.example.guangnian.feature.console.ui.panel.MapUtils.pixelToWorld
import com.example.guangnian.feature.console.ui.panel.MapUtils.worldToPixel
import kotlin.math.*

@Composable
fun MapBackgroundLayer(
    uiState: MapUiState,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val config = uiState.mapConfig
    var scale by remember(config) { mutableFloatStateOf(1f) }
    var offset by remember(config) { mutableStateOf(Offset.Zero) }
    var isInitialized by remember(config) { mutableStateOf(false) }

    val mapBitmap = uiState.mapImage
    val textMeasurer = rememberTextMeasurer()
    val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .pointerInput(config) {
                    if (config == null) return@pointerInput
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.1f, 10f)
                        offset = centroid + pan - (centroid - offset) * (newScale / oldScale)
                        scale = newScale
                    }
                }
                .pointerInput(config, uiState.interactionMode, scale, offset, mapBitmap) {
                    if (config == null) return@pointerInput
                    detectTapGestures(
                        onLongPress = { screenOffset ->
                            val mapPixelOffset = (screenOffset - offset) / scale
                            val worldPos = pixelToWorld(mapPixelOffset, config, mapHeightPx)
                            KLog.d("MapDebug", "长按创建点位: 屏幕=$screenOffset, 像素=$mapPixelOffset, 世界=$worldPos")
                            mapViewModel.onMapLongClick(screenOffset, worldPos)
                        },
                        onTap = { screenOffset ->
                            when (uiState.interactionMode) {
                                MapInteractionMode.PICK_ORIENTATION -> {
                                    val mapPixelOffset = (screenOffset - offset) / scale
                                    val worldPos = pixelToWorld(mapPixelOffset, config, mapHeightPx)
                                    mapViewModel.onMapClickForOrientation(screenOffset, worldPos)
                                }
                                MapInteractionMode.PICK_POSITION -> {
                                    val mapPixelOffset = (screenOffset - offset) / scale
                                    val worldPos = pixelToWorld(mapPixelOffset, config, mapHeightPx)
                                    mapViewModel.onMapClickForPosition(worldPos)
                                }
                                MapInteractionMode.CONFIRM_POINT -> {
                                }
                                else -> {
                                    val adaptiveFactor = 1f / sqrt(scale)
                                    val screenPointRadius = (12f * adaptiveFactor) * scale
                                    val clickRadius = (screenPointRadius * 2.5f).coerceAtLeast(100f)
                                    
                                    val clickedPoint = uiState.endpoints.find { ep ->
                                        val realX = ep.position?.x ?: ep.x
                                        val realY = ep.position?.y ?: ep.y
                                        val px = worldToPixel(realX, realY, config, mapHeightPx)
                                        val spx = px.x * scale + offset.x
                                        val spy = px.y * scale + offset.y
                                        hypot(screenOffset.x - spx, screenOffset.y - spy) < clickRadius
                                    }
                                    
                                    if (clickedPoint != null) {
                                        mapViewModel.onPointClick(screenOffset, clickedPoint)
                                    } else {
                                        mapViewModel.clearInteraction()
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()

            LaunchedEffect(mapBitmap, containerWidth, containerHeight) {
                if (mapBitmap != null && !isInitialized && containerWidth > 0 && containerHeight > 0) {
                    val bmpW = mapBitmap.width.toFloat()
                    val bmpH = mapBitmap.height.toFloat()
                    val scaleW = containerWidth / bmpW
                    val scaleH = containerHeight / bmpH
                    val targetScale = (if (scaleW < scaleH) scaleW else scaleH) * 0.9f
                    val finalScale = if (targetScale < 1.0f) targetScale else 1.0f
                    scale = finalScale
                    offset = Offset((containerWidth - bmpW * finalScale) / 2f, (containerHeight - bmpH * finalScale) / 2f)
                    isInitialized = true
                }
            }

            val layerModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }

            StaticMapLayer(mapBitmap, layerModifier)
            PathLayer(uiState.globalPath, uiState.localPath, config, mapHeightPx, scale, layerModifier)
            MarkerLayer(uiState.endpoints, config, mapHeightPx, scale, textMeasurer, layerModifier)
            RobotLayer(uiState.robotPosition, uiState.pointCloud, config, mapHeightPx, scale, layerModifier)
            InteractionLayer(uiState.interactionMode, uiState.tempPoint, uiState.tempYaw, config, mapHeightPx, scale, layerModifier)

            if (config == null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFallbackGrid()
                }
            }
        }

        MapControlOverlays(uiState, mapViewModel, config, mapHeightPx, scale, offset)
    }
}

@Composable
private fun StaticMapLayer(mapBitmap: ImageBitmap?, modifier: Modifier) {
    if (mapBitmap == null) return
    Canvas(modifier = modifier) {
        drawImage(image = mapBitmap, topLeft = Offset.Zero)
    }
}

@Composable
private fun PathLayer(
    globalPath: List<Point2DBean>,
    localPath: List<Point2DBean>,
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    modifier: Modifier
) {
    if (config == null) return
    Canvas(modifier = modifier) {
        drawPathLine(globalPath, config, mapHeightPx, Color(0xFF03DC6B), 5f / scale)
        drawPathLine(localPath, config, mapHeightPx, Color(0xFFF3D65A), 6f / scale)
    }
}

@Composable
private fun MarkerLayer(
    endpoints: List<InspectionPointBean>,
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    textMeasurer: TextMeasurer,
    modifier: Modifier
) {
    if (config == null) return
    Canvas(modifier = modifier) {
        endpoints.forEach { ep ->
            val realX = ep.position?.x ?: ep.x
            val realY = ep.position?.y ?: ep.y
            val realYaw = ep.position?.yaw ?: ep.yaw
            val px = worldToPixel(realX, realY, config, mapHeightPx)
            val pointColor = when (ep.pointType) {
                1 -> Color(0xFFFFD600)
                2 -> Color(0xFF4CAF50)
                else -> Color(0xFF9C27B0)
            }
            val adaptiveFactor = 1f / sqrt(scale)
            val pointRadius = (12f * adaptiveFactor).coerceAtLeast(10f / scale)
            
            drawCircle(color = pointColor, radius = pointRadius, center = px)
            drawCircle(color = Color.White, radius = pointRadius + 2f * adaptiveFactor, center = px, style = Stroke(width = 2f * adaptiveFactor))
            
            val arrowLength = (18f * adaptiveFactor).coerceAtLeast(15f / scale)
            drawDirectionArrow(px, arrowLength, realYaw, pointColor, offset = pointRadius * 1.4f)

            val textLayoutResult = textMeasurer.measure(
                text = ep.name ?: "",
                style = TextStyle(
                    color = Color.White,
                    fontSize = (6.sp.value * adaptiveFactor).sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(1.5f * adaptiveFactor, 1.5f * adaptiveFactor),
                        blurRadius = 3f * adaptiveFactor
                    )
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = px.x - textLayoutResult.size.width / 2,
                    y = px.y + pointRadius + 2.5f * adaptiveFactor
                )
            )
        }
    }
}

@Composable
private fun RobotLayer(
    robotPosition: RobotPositionBean?,
    pointCloud: List<Point2DBean>,
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    modifier: Modifier
) {
    if (config == null || robotPosition == null) return

    val worldCloud = remember(pointCloud, robotPosition) {
        MapTransformUtils.calculatePointCloudWorldCoords(pointCloud, robotPosition)
    }

    Canvas(modifier = modifier) {
        val px = worldToPixel(robotPosition.x, robotPosition.y, config, mapHeightPx)
        val adaptiveFactor = 1f / sqrt(scale)
        val robotRadius = (12f * adaptiveFactor).coerceAtLeast(10f / scale)
        drawCircle(color = Color(0xFFFFA500), radius = robotRadius, center = px)
        val arrowLength = (16f * adaptiveFactor).coerceAtLeast(14f / scale)
        drawDirectionArrow(px, arrowLength, robotPosition.yaw, Color.Red, offset = robotRadius * 0.5f)

        if (worldCloud.isNotEmpty()) {
            val points = worldCloud.map { worldToPixel(it.x, it.y, config, mapHeightPx) }
            drawPoints(
                points = points,
                pointMode = PointMode.Points,
                color = Color.Red.copy(alpha = 0.8f),
                strokeWidth = 6f / scale,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun InteractionLayer(
    interactionMode: MapInteractionMode,
    tempPoint: Point2DBean?,
    tempYaw: Float,
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    modifier: Modifier
) {
    if (config == null) return
    if ((interactionMode == MapInteractionMode.PICK_ORIENTATION || interactionMode == MapInteractionMode.CONFIRM_POINT) && tempPoint != null) {
        Canvas(modifier = modifier) {
            val px = worldToPixel(tempPoint.x, tempPoint.y, config, mapHeightPx)
            val adaptiveFactor = 1f / sqrt(scale)
            drawCircle(color = Color(0xFF007AFF), radius = 14f * adaptiveFactor, center = px)
            drawDirectionArrow(px, 20f * adaptiveFactor, tempYaw.toDouble(), Color(0xFF007AFF), offset = 16f * adaptiveFactor)
        }
    }
}

@Composable
private fun MapControlOverlays(
    uiState: MapUiState,
    mapViewModel: MapViewModel,
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    offset: Offset
) {
    if (uiState.interactionMode == MapInteractionMode.SHOW_MENU && uiState.menuPosition != null) {
        Popup(
            alignment = Alignment.TopStart,
            offset = androidx.compose.ui.unit.IntOffset(uiState.menuPosition.x.toInt(), uiState.menuPosition.y.toInt()),
            onDismissRequest = { mapViewModel.clearInteraction() }
        ) {
            GlassBox(modifier = Modifier.width(120.dp), bgColor = Color(0xEE2C2C2C)) {
                Column {
                    Text(
                        "创建导览点",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onSelectCreatePoint() }.padding(12.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "去此处",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().clickable { /* TODO */ }.padding(12.dp)
                    )
                }
            }
        }
    }
    
    if (uiState.interactionMode == MapInteractionMode.CONFIRM_POINT && uiState.tempPoint != null && config != null) {
        val px = worldToPixel(uiState.tempPoint.x, uiState.tempPoint.y, config, mapHeightPx)
        val screenX = px.x * scale + offset.x
        val screenY = px.y * scale + offset.y
        val density = androidx.compose.ui.platform.LocalDensity.current
        val popupWidthPx = with(density) { 140.dp.toPx() }

        Popup(
            alignment = Alignment.TopStart,
            offset = androidx.compose.ui.unit.IntOffset(
                (screenX - popupWidthPx / 2).toInt(),
                (screenY + 30).toInt()
            ),
            onDismissRequest = { mapViewModel.clearInteraction() }
        ) {
            GlassBox(modifier = Modifier.width(140.dp), bgColor = Color(0xEE2C2C2C)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "取消",
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { mapViewModel.clearInteraction() }.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                    VerticalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.2f))
                    Text(
                        "确认",
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { mapViewModel.onConfirmCreatePoint() }.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
    
    if (uiState.interactionMode == MapInteractionMode.PICK_ORIENTATION) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 100.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xAA000000)).padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("请在地图上点击一个位置以确定方向", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "取消",
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { mapViewModel.clearInteraction() }
                    )
                }
            }
        }
    }

    if (uiState.interactionMode == MapInteractionMode.PICK_POSITION) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 100.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xAA000000)).padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("请在地图上点击新位置", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "取消",
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { mapViewModel.clearInteraction() }
                    )
                }
            }
        }
    }

    if (uiState.interactionMode == MapInteractionMode.POINT_MENU && uiState.menuPosition != null && uiState.selectedPoint != null) {
        Popup(
            alignment = Alignment.TopStart,
            offset = androidx.compose.ui.unit.IntOffset(uiState.menuPosition.x.toInt(), uiState.menuPosition.y.toInt()),
            onDismissRequest = { mapViewModel.clearInteraction() }
        ) {
            GlassBox(modifier = Modifier.width(110.dp), bgColor = Color(0xEE2C2C2C)) {
                Column {
                    Text(
                        "修改信息",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onEditPoint() }.padding(10.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "重设位置",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onResetPointPosition() }.padding(10.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "删除点位",
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onDeletePoint() }.padding(10.dp)
                    )
                }
            }
        }
    }
}
