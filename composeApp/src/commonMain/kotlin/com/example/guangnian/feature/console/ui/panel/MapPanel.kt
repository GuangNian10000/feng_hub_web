package com.example.guangnian.feature.console.ui.panel

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import androidx.compose.material.CircularProgressIndicator
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.bean.*
import com.example.guangnian.core.designsystem.components.GlassBox
import com.example.guangnian.feature.console.utils.MapTransformUtils
import com.example.guangnian.feature.console.viewmodel.MapInteractionMode
import com.example.guangnian.feature.console.viewmodel.MapUiState
import com.example.guangnian.feature.console.viewmodel.MapViewModel
import com.example.guangnian.feature.console.utils.MapUtils.drawDirectionArrow
import com.example.guangnian.feature.console.utils.MapUtils.drawFallbackGrid
import com.example.guangnian.feature.console.utils.MapUtils.drawPathLine
import kotlin.math.*

@Composable
fun MapBackgroundLayer(
    uiState: MapUiState,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val config = uiState.mapConfig
    // 稳定性修复：使用地图图片 URL 作为标识，只有切换地图时才重置状态
    // 建图模式下，image 路径可能会包含实时更新的参数，我们只取路径部分作为唯一标识
    val mapId = remember(config?.image) { 
        config?.image?.substringBefore("?") ?: "none" 
    }
    
    var scale by remember(mapId) { mutableFloatStateOf(1f) }
    var offset by remember(mapId) { mutableStateOf(Offset.Zero) }
    var rotation by remember(mapId) { mutableFloatStateOf(0f) }
    var isInitialized by remember(mapId) { mutableStateOf(false) }

    val mapBitmap = uiState.mapImage
    val textMeasurer = rememberTextMeasurer()
    val mapHeightPx = mapBitmap?.height?.toFloat() ?: 1000f

    // 使用 rememberUpdatedState 避免 pointerInput 因为频繁变更的 scale/offset 而重新启动，解决抖动问题
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)
    val currentRotation by rememberUpdatedState(rotation)
    val currentMapBitmap by rememberUpdatedState(mapBitmap)

    // 辅助函数：将屏幕坐标转换为地图像素坐标（考虑缩放、偏移和旋转）
    val screenToMapPixel = { screenOffset: Offset ->
        val rad = -currentRotation * (PI / 180).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val relativeOffset = screenOffset - currentOffset
        val unrotatedX = relativeOffset.x * cosA - relativeOffset.y * sinA
        val unrotatedY = relativeOffset.x * sinA + relativeOffset.y * cosA
        Offset(unrotatedX, unrotatedY) / currentScale
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .pointerInput(mapId) { // 使用稳定的 mapId
                    if (config == null) return@pointerInput
                    detectTransformGestures { centroid, pan, zoom, rotationChange ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.1f, 10f)
                        
                        // 更新旋转角度
                        rotation += rotationChange
                        
                        // 计算缩放和旋转后的偏移量，使其围绕手势中心点变换
                        val centeredOffset = offset - centroid
                        val rad = rotationChange * (PI / 180).toFloat()
                        val cosA = cos(rad)
                        val sinA = sin(rad)
                        val rotatedX = centeredOffset.x * cosA - centeredOffset.y * sinA
                        val rotatedY = centeredOffset.x * sinA + centeredOffset.y * cosA
                        
                        offset = centroid + pan + Offset(rotatedX, rotatedY) * (newScale / oldScale)
                        scale = newScale
                    }
                }
                .pointerInput(mapId, uiState.interactionMode) { 
                    val currentConfig = config ?: return@pointerInput
                    detectTapGestures(
                        onLongPress = { screenOffset ->
                            val mapPixelOffset = screenToMapPixel(screenOffset)
                            val worldPos = MapTransformUtils.pixelToWorld(mapPixelOffset, currentConfig, mapHeightPx)
                            KLog.d("MapDebug", "长按创建点位: 屏幕=$screenOffset, 像素=$mapPixelOffset, 世界=$worldPos")
                            mapViewModel.onMapLongClick(screenOffset, worldPos)
                        },
                        onTap = { screenOffset ->
                            when (uiState.interactionMode) {
                                MapInteractionMode.PICK_ORIENTATION, MapInteractionMode.PICK_ROBOT_ORIENTATION, MapInteractionMode.PICK_GO_HERE_ORIENTATION -> {
                                    val mapPixelOffset = screenToMapPixel(screenOffset)
                                    val worldPos = MapTransformUtils.pixelToWorld(mapPixelOffset, currentConfig, mapHeightPx)
                                    mapViewModel.onMapClickForOrientation(screenOffset, worldPos)
                                }
                                MapInteractionMode.PICK_POSITION -> {
                                    val mapPixelOffset = screenToMapPixel(screenOffset)
                                    val worldPos = MapTransformUtils.pixelToWorld(mapPixelOffset, currentConfig, mapHeightPx)
                                    mapViewModel.onMapClickForPosition(worldPos)
                                }
                                MapInteractionMode.CONFIRM_POINT, MapInteractionMode.CONFIRM_ROBOT_POSITION -> {
                                }
                                else -> {
                                    val adaptiveFactor = 1f / sqrt(currentScale)
                                    val screenPointRadius = (12f * adaptiveFactor) * currentScale
                                    val clickRadius = (screenPointRadius * 2.5f).coerceAtLeast(100f)

                                    val clickedPoint = uiState.endpoints.find { ep ->
                                        val realX = ep.position?.x ?: ep.x
                                        val realY = ep.position?.y ?: ep.y
                                        val px = MapTransformUtils.worldToPixel(realX, realY, currentConfig, mapHeightPx)
                                        
                                        // 将像素坐标转回屏幕坐标进行点击判定
                                        val rad = currentRotation * (PI / 180).toFloat()
                                        val cosA = cos(rad)
                                        val sinA = sin(rad)
                                        val scaled = px * currentScale
                                        val spx = (scaled.x * cosA - scaled.y * sinA) + currentOffset.x
                                        val spy = (scaled.x * sinA + scaled.y * cosA) + currentOffset.y
                                        
                                        hypot(screenOffset.x - spx, screenOffset.y - spy) < clickRadius
                                    }
                                    
                                    if (clickedPoint != null) {
                                        mapViewModel.onPointClick(screenOffset, clickedPoint)
                                    } else {
                                        val isClickedRobot = uiState.robotPosition?.let { robot ->
                                            val px = MapTransformUtils.worldToPixel(robot.x, robot.y, currentConfig, mapHeightPx)
                                            val rad = currentRotation * (PI / 180).toFloat()
                                            val cosA = cos(rad)
                                            val sinA = sin(rad)
                                            val scaled = px * currentScale
                                            val spx = (scaled.x * cosA - scaled.y * sinA) + currentOffset.x
                                            val spy = (scaled.x * sinA + scaled.y * cosA) + currentOffset.y
                                            hypot(screenOffset.x - spx, screenOffset.y - spy) < clickRadius
                                        } ?: false

                                        if (!isClickedRobot) {
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

            // 居中逻辑：仅在首次加载新地图（mapId 改变且 isInitialized 为 false）且图片加载完成时执行
            LaunchedEffect(mapId, mapBitmap, containerWidth, containerHeight, isInitialized) {
                if (mapBitmap != null && !isInitialized && containerWidth > 0 && containerHeight > 0) {
                    val bmpW = mapBitmap.width.toFloat()
                    val bmpH = mapBitmap.height.toFloat()
                    val scaleW = containerWidth / bmpW
                    val scaleH = containerHeight / bmpH
                    // 默认放大显示，设为自适应填充倍率的 3.0 倍，确保地图清晰且足够大
                    val finalScale = (if (scaleW < scaleH) scaleW else scaleH) * 1.0f

                    scale = finalScale
                    rotation = 0f
                    offset = Offset((containerWidth - bmpW * finalScale) / 2f, (containerHeight - bmpH * finalScale) / 2f)
                    isInitialized = true
                }
            }

            val layerModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }

            StaticMapLayer(mapBitmap, layerModifier)
            
            if (mapBitmap != null) { // 移除 !uiState.isMapLoading 判断，避免刷新时图层消失导致的闪烁
                PathLayer(uiState.globalPath, uiState.localPath, config, mapHeightPx, scale, layerModifier)
                if (uiState.navStatus.state == RobotNavState.MAPPING) {
                    MappingTrajectoryLayer(uiState.mappingTrajectory, config, mapHeightPx, scale, layerModifier)
                }
                // 调整层级：点云放在下方
                PointCloudLayer(uiState.robotPosition, uiState.robotPosition?.let { uiState.pointCloud } ?: emptyList(), config, mapHeightPx, scale, layerModifier)
                // 导览点在点云之上
                MarkerLayer(uiState.endpoints, config, mapHeightPx, scale, textMeasurer, layerModifier)
                // 机器人图标在最上层
                RobotLayer(uiState.robotPosition, config, mapHeightPx, scale, layerModifier)
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
                        progress = progress,
                        modifier = Modifier.size(56.dp),
                        color = Color(0xFF007AFF),
                        strokeWidth = 4.dp,
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
        // 增加路径粗细：全局路径从 5f 改为 12f，局部路径从 6f 改为 15f
        drawPathLine(globalPath, config, mapHeightPx, Color(0xFF03DC6B), 17f / scale)
        drawPathLine(localPath, config, mapHeightPx, Color(0xFFF3D65A), 20f / scale)
    }
}

@Composable
private fun MappingTrajectoryLayer(
    trajectory: List<Point2DBean>,
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    modifier: Modifier
) {
    if (config == null || trajectory.size < 2) return
    Canvas(modifier = modifier) {
        drawPathLine(trajectory, config, mapHeightPx, Color(0xFF007AFF), 12f / scale)
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
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    modifier: Modifier
) {
    if (config == null || robotPosition == null) return

    Canvas(modifier = modifier) {
        val px = MapTransformUtils.worldToPixel(robotPosition.x, robotPosition.y, config, mapHeightPx)
        val adaptiveFactor = 1f / sqrt(scale)
        val robotRadius = (12f * adaptiveFactor).coerceAtLeast(10f / scale)
        drawCircle(color = Color(0xFFFFA500), radius = robotRadius, center = px)
        val arrowLength = (16f * adaptiveFactor).coerceAtLeast(14f / scale)
        drawDirectionArrow(px, arrowLength, robotPosition.yaw, Color.Red, offset = robotRadius * 1.4f)
    }
}

@Composable
private fun PointCloudLayer(
    robotPosition: RobotPositionBean?,
    pointCloud: List<Point2DBean>,
    config: MapConfigBean?,
    mapHeightPx: Float,
    scale: Float,
    modifier: Modifier
) {
    if (config == null || robotPosition == null || pointCloud.isEmpty()) return

    val worldCloud = remember(pointCloud, robotPosition) {
        MapTransformUtils.calculatePointCloudWorldCoords(pointCloud, robotPosition)
    }

    Canvas(modifier = modifier) {
        if (worldCloud.isNotEmpty()) {
            val points = worldCloud.map { MapTransformUtils.worldToPixel(it.x, it.y, config, mapHeightPx) }
            drawPoints(
                points = points,
                pointMode = PointMode.Points,
                color = Color.Red.copy(alpha = 0.6f), // 稍微调低透明度减少干扰
                strokeWidth = 4.0f,
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
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "设置机器人位置",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onSelectSetRobotPosition() }.padding(12.dp)
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
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
                    Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.White.copy(alpha = 0.2f))
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
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "修改信息",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onEditPoint() }.padding(10.dp)
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "重设位置",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { mapViewModel.onResetPointPosition() }.padding(10.dp)
                    )
                    Divider(color = Color.White.copy(alpha = 0.1f))
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
