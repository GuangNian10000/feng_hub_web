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
import androidx.compose.material3.CircularProgressIndicator
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
import kotlin.math.*

@Composable
fun MapBackgroundLayer(
    uiState: MapUiState,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val config = uiState.mapConfig
    // 稳定性修复：不直接以 config 对象为 key，避免轮询导致的状态重置
    // 只有当坐标系关键参数变化时才重置缩放和位移
    val configIdentity = remember(config) {
        config?.let { "${it.origin}_${it.resolution}" } ?: "none"
    }
    
    var scale by remember(configIdentity) { mutableFloatStateOf(1f) }
    var offset by remember(configIdentity) { mutableStateOf(Offset.Zero) }
    var isInitialized by remember(configIdentity) { mutableStateOf(false) }

    val mapBitmap = uiState.mapImage
    val textMeasurer = rememberTextMeasurer()
    val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .pointerInput(uiState.mapConfig) {
                    if (uiState.mapConfig == null) return@pointerInput
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.1f, 10f)
                        offset = centroid + pan - (centroid - offset) * (newScale / oldScale)
                        scale = newScale
                    }
                }
                .pointerInput(uiState.mapConfig, uiState.interactionMode, scale, offset, mapBitmap) {
                    val currentConfig = uiState.mapConfig ?: return@pointerInput
                    detectTapGestures(
                        onLongPress = { screenOffset ->
                            val mapPixelOffset = (screenOffset - offset) / scale
                            val worldPos = MapTransformUtils.pixelToWorld(mapPixelOffset, currentConfig, mapHeightPx)
                            KLog.d("MapDebug", "长按创建点位: 屏幕=$screenOffset, 像素=$mapPixelOffset, 世界=$worldPos")
                            mapViewModel.onMapLongClick(screenOffset, worldPos)
                        },
                        onTap = { screenOffset ->
                            when (uiState.interactionMode) {
                                MapInteractionMode.PICK_ORIENTATION, MapInteractionMode.PICK_ROBOT_ORIENTATION, MapInteractionMode.PICK_GO_HERE_ORIENTATION -> {
                                    val mapPixelOffset = (screenOffset - offset) / scale
                                    val worldPos = MapTransformUtils.pixelToWorld(mapPixelOffset, currentConfig, mapHeightPx)
                                    mapViewModel.onMapClickForOrientation(screenOffset, worldPos)
                                }
                                MapInteractionMode.PICK_POSITION -> {
                                    val mapPixelOffset = (screenOffset - offset) / scale
                                    val worldPos = MapTransformUtils.pixelToWorld(mapPixelOffset, currentConfig, mapHeightPx)
                                    mapViewModel.onMapClickForPosition(worldPos)
                                }
                                MapInteractionMode.CONFIRM_POINT, MapInteractionMode.CONFIRM_ROBOT_POSITION -> {
                                }
                                else -> {
                                    val adaptiveFactor = 1f / sqrt(scale)
                                    val screenPointRadius = (12f * adaptiveFactor) * scale
                                    val clickRadius = (screenPointRadius * 2.5f).coerceAtLeast(100f)

                                    val clickedPoint = uiState.endpoints.find { ep ->
                                        val realX = ep.position?.x ?: ep.x
                                        val realY = ep.position?.y ?: ep.y
                                        val px = MapTransformUtils.worldToPixel(realX, realY, currentConfig, mapHeightPx)
                                        val spx = px.x * scale + offset.x
                                        val spy = px.y * scale + offset.y
                                        hypot(screenOffset.x - spx, screenOffset.y - spy) < clickRadius
                                    }
                                    
                                    if (clickedPoint != null) {
                                        // 如果点击到了导览点，正常响应（即使此处有机器人）
                                        mapViewModel.onPointClick(screenOffset, clickedPoint)
                                    } else {
                                        // 如果没点击到导览点，检测是否点击了机器人
                                        val isClickedRobot = uiState.robotPosition?.let { robot ->
                                            val px = MapTransformUtils.worldToPixel(robot.x, robot.y, currentConfig, mapHeightPx)
                                            val spx = px.x * scale + offset.x
                                            val spy = px.y * scale + offset.y
                                            hypot(screenOffset.x - spx, screenOffset.y - spy) < clickRadius
                                        } ?: false

                                        if (isClickedRobot) {
                                            // 机器人位置不可点击，不做任何处理（不触发弹窗，也不清除当前交互）
                                            // 或者也可以调用 clearInteraction() 取决于交互习惯，这里选择不做操作以符合“点不了”的直觉
                                        } else {
                                            mapViewModel.clearInteraction()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()

            // 只有当关键参数变化时，才重新初始化变换
            LaunchedEffect(configIdentity, containerWidth, containerHeight) {
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
            
            if (!uiState.isMapLoading && mapBitmap != null) {
                PathLayer(uiState.globalPath, uiState.localPath, config, mapHeightPx, scale, layerModifier)
                MarkerLayer(uiState.endpoints, config, mapHeightPx, scale, textMeasurer, layerModifier)
                RobotLayer(uiState.robotPosition, uiState.pointCloud, config, mapHeightPx, scale, layerModifier)
                InteractionLayer(uiState.interactionMode, uiState.tempPoint, uiState.tempYaw, config, mapHeightPx, scale, layerModifier)
            }

            if (config == null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFallbackGrid()
                }
            }
        }

        if (uiState.isMapLoading) {
            MapLoadingOverlay(uiState.mapLoadingProgress, uiState.loadingStage)
        }

        MapControlOverlays(uiState, mapViewModel, config, mapHeightPx, scale, offset)
    }
}

@Composable
private fun MapLoadingOverlay(progress: Float, stage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        GlassBox(
            modifier = Modifier.padding(20.dp),
            bgColor = Color(0xCC1E1E1E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        color = Color(0xFF007AFF),
                        strokeWidth = 4.dp,
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = stage,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
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
            val px = MapTransformUtils.worldToPixel(realX, realY, config, mapHeightPx)
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
        val px = MapTransformUtils.worldToPixel(robotPosition.x, robotPosition.y, config, mapHeightPx)
        val adaptiveFactor = 1f / sqrt(scale)
        val robotRadius = (12f * adaptiveFactor).coerceAtLeast(10f / scale)
        drawCircle(color = Color(0xFFFFA500), radius = robotRadius, center = px)
        val arrowLength = (16f * adaptiveFactor).coerceAtLeast(14f / scale)
        drawDirectionArrow(px, arrowLength, robotPosition.yaw, Color.Red, offset = robotRadius * 1.4f)

        if (worldCloud.isNotEmpty()) {
            val points = worldCloud.map { MapTransformUtils.worldToPixel(it.x, it.y, config, mapHeightPx) }
            drawPoints(
                points = points,
                pointMode = PointMode.Points,
                color = Color.Red.copy(alpha = 0.8f),
                strokeWidth = 6.0f,
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
    if ((interactionMode == MapInteractionMode.PICK_ORIENTATION || 
        interactionMode == MapInteractionMode.CONFIRM_POINT ||
        interactionMode == MapInteractionMode.PICK_ROBOT_ORIENTATION ||
        interactionMode == MapInteractionMode.CONFIRM_ROBOT_POSITION ||
        interactionMode == MapInteractionMode.PICK_GO_HERE_ORIENTATION ||
        interactionMode == MapInteractionMode.CONFIRM_GO_HERE) && tempPoint != null) {
        Canvas(modifier = modifier) {
            val px = MapTransformUtils.worldToPixel(tempPoint.x, tempPoint.y, config, mapHeightPx)
            val adaptiveFactor = 1f / sqrt(scale)
            val color = if (interactionMode == MapInteractionMode.PICK_ROBOT_ORIENTATION || 
                           interactionMode == MapInteractionMode.CONFIRM_ROBOT_POSITION) Color(0xFFFFA500) else Color(0xFF007AFF)
            drawCircle(color = color, radius = 14f * adaptiveFactor, center = px)
            drawDirectionArrow(px, 20f * adaptiveFactor, tempYaw.toDouble(), color, offset = 16f * adaptiveFactor)
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
                        "设置机器人位置",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onSelectSetRobotPosition() }.padding(12.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "去此处",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onSelectGoHere() }.padding(12.dp)
                    )
                }
            }
        }
    }
    
    if ((uiState.interactionMode == MapInteractionMode.CONFIRM_POINT || uiState.interactionMode == MapInteractionMode.CONFIRM_ROBOT_POSITION || uiState.interactionMode == MapInteractionMode.CONFIRM_GO_HERE) 
        && uiState.tempPoint != null && config != null) {
        val px = MapTransformUtils.worldToPixel(uiState.tempPoint.x, uiState.tempPoint.y, config, mapHeightPx)
        val screenX = px.x * scale + offset.x
        val screenY = px.y * scale + offset.y
        val density = androidx.compose.ui.platform.LocalDensity.current
        val popupWidthPx = with(density) { 140.dp.toPx() }

        Popup(
            alignment = Alignment.TopStart,
            offset = androidx.compose.ui.unit.IntOffset(
                (screenX - popupWidthPx / 2).toInt(),
                (screenY + 60).toInt()
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
                        modifier = Modifier.clickable { 
                            when (uiState.interactionMode) {
                                MapInteractionMode.CONFIRM_ROBOT_POSITION -> mapViewModel.onConfirmSetRobotPosition()
                                MapInteractionMode.CONFIRM_GO_HERE -> mapViewModel.onConfirmGoHere()
                                else -> mapViewModel.onConfirmCreatePoint()
                            }
                        }.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
    
    if (uiState.interactionMode == MapInteractionMode.PICK_ORIENTATION || 
        uiState.interactionMode == MapInteractionMode.PICK_ROBOT_ORIENTATION ||
        uiState.interactionMode == MapInteractionMode.PICK_GO_HERE_ORIENTATION) {
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
                        "前往该点",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onGoToPoint() }.padding(10.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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
