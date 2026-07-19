package com.example.guangnian.feature.console.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.utils.PgmUtils
import com.example.guangnian.feature.console.data.api.NavigationApi
import com.example.guangnian.feature.console.data.bean.*
import com.example.guangnian.feature.console.data.event.RobotEvent
import com.example.guangnian.feature.console.data.event.RobotEventBus
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.time.Clock

data class MapUiState(
    val mapConfig: MapConfigBean? = null,
    val mapImage: ImageBitmap? = null,
    val robotPosition: RobotPositionBean? = null,
    val endpoints: List<InspectionPointBean> = emptyList(),
    val globalPath: List<Point2DBean> = emptyList(),
    val localPath: List<Point2DBean> = emptyList(),
    val pointCloud: List<Point2DBean> = emptyList(),
    val isMapping: Boolean = false,
    // 交互状态
    val interactionMode: MapInteractionMode = MapInteractionMode.NONE,
    val selectedPoint: InspectionPointBean? = null,
    val menuPosition: androidx.compose.ui.geometry.Offset? = null,
    val tempPoint: Point2DBean? = null,
    val tempYaw: Float = 0f,
    // 加载状态
    val isMapLoading: Boolean = false,
    val mapLoadingProgress: Float = 0f,
    val loadingStage: String = ""
)

enum class MapInteractionMode {
    NONE, SHOW_MENU, PICK_POSITION, PICK_ORIENTATION, CONFIRM_POINT, POINT_MENU,
    PICK_ROBOT_ORIENTATION, CONFIRM_ROBOT_POSITION, PICK_GO_HERE_ORIENTATION, CONFIRM_GO_HERE
}

class MapViewModel(
    private val navigationApi: NavigationApi,
    private val settings: Settings,
    private val httpClient: HttpClient,
    private val eventBus: RobotEventBus
) : ViewModel() {
    private companion object {
        const val TAG = "MapViewModel"
    }

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private var mapConfigSeq = 0L
    private var positionSeq = 0L
    private var pathSeq = 0L
    private var pointsSeq = 0L
    private var imageSeq = 0L

    init {
        startMapDataPolling()
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is RobotEvent.PointsChanged -> {
                        KLog.d(TAG, "收到点位变更事件，刷新地图点位")
                        refreshMapData()
                    }
                    is RobotEvent.MappingStateChanged -> {
                        KLog.d(TAG, "收到建图状态变更: isMapping=${event.isMapping}")
                        _uiState.update { it.copy(isMapping = event.isMapping) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startMapDataPolling() {
        // 1. 地图配置与图片加载
        viewModelScope.launch {
            while (isActive) {
                mapConfigSeq += 1
                val seq = mapConfigSeq
                val startTime = Clock.System.now().toEpochMilliseconds()
                try {
                    val config = navigationApi.getMapConfig()
                    val savedIp = settings.getString("saved_ip", "192.168.1.100")
                    val fixedUrl = config.getFixedImageUrl(savedIp)
                    val needLoadImage = _uiState.value.mapConfig == null || _uiState.value.isMapping
                    
                    // 如果是第一次加载或者正在建图模式，持续更新配置和图片
                    if (needLoadImage) {
                        _uiState.update { it.copy(mapConfig = config.copy(image = fixedUrl)) }
                        loadMapImage(fixedUrl)
                    }
                    KLog.d(
                        TAG,
                        "地图配置同步成功 #$seq: resolution=${config.resolution}, origin=${config.origin}, image=$fixedUrl, needLoadImage=$needLoadImage, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                    )
                } catch (e: Exception) {
                    KLog.w(TAG, "地图配置同步失败 #$seq: ${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                }
                
                // 如果正在建图，地图刷新频率可以适当高一点（例如 3s 一次）
                // 如果不在建图，且地图已加载，则可以停止轮询或极低频率
                if (_uiState.value.mapConfig != null && !_uiState.value.isMapping) {
                    delay(10000)
                } else {
                    delay(3000)
                }
            }
        }

        // 2. 高频位置刷新 (5Hz)
        viewModelScope.launch {
            while (isActive) {
                positionSeq += 1
                val seq = positionSeq
                val startTime = Clock.System.now().toEpochMilliseconds()
                try {
                    val pos = navigationApi.getPosition()
                    _uiState.update { it.copy(robotPosition = pos) }
                    KLog.d(
                        TAG,
                        "机器人位置同步成功 #$seq: x=${pos.x}, y=${pos.y}, yaw=${pos.yaw}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                    )
                } catch (e: Exception) {
                    KLog.w(TAG, "机器人位置同步失败 #$seq: ${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                    delay(3000) //500
                }
                delay(3000) //200
            }
        }

        // 3. 动效数据刷新 (1Hz: 点云、路径)
        viewModelScope.launch {
            while (isActive) {
                pathSeq += 1
                val seq = pathSeq
                val startTime = Clock.System.now().toEpochMilliseconds()
                try {
                    coroutineScope {
                        val cloudDef = async {
                            try {
                                navigationApi.getPointCloud()
                            } catch(e: Exception) {
                                KLog.w(TAG, "点云同步失败 #$seq: ${e.message}", e)
                                emptyList<Point2DBean>()
                            }
                        }
                        val globalDef = async {
                            try {
                                navigationApi.getGlobalPath().points
                            } catch(e: Exception) {
                                KLog.w(TAG, "全局路径同步失败 #$seq: ${e.message}", e)
                                emptyList<Point2DBean>()
                            }
                        }
                        val localDef = async {
                            try {
                                navigationApi.getLocalPath().points
                            } catch(e: Exception) {
                                KLog.w(TAG, "局部路径同步失败 #$seq: ${e.message}", e)
                                emptyList<Point2DBean>()
                            }
                        }
                        val pointCloud = cloudDef.await()
                        val globalPath = globalDef.await()
                        val localPath = localDef.await()
                        
                        _uiState.update { it.copy(
                            pointCloud = pointCloud,
                            globalPath = globalPath,
                            localPath = localPath
                        ) }
                        KLog.d(
                            TAG,
                            "路径数据同步完成 #$seq: pointCloud=${pointCloud.size}, globalPath=${globalPath.size}, localPath=${localPath.size}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                        )
                    }
                } catch (e: Exception) {
                    KLog.w(TAG, "路径数据同步异常 #$seq: ${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                }
                delay(3000)
            }
        }

        // 4. 导览点数据轮询 (1Hz)
        viewModelScope.launch {
            while (isActive) {
                pointsSeq += 1
                val seq = pointsSeq
                val startTime = Clock.System.now().toEpochMilliseconds()
                try {
                    val points = navigationApi.getInspectionPoints()
                    _uiState.update { it.copy(endpoints = points) }
                    KLog.d(
                        TAG,
                        "导览点同步成功 #$seq: count=${points.size}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms"
                    )
                } catch (e: Exception) {
                    KLog.w(TAG, "导览点同步失败 #$seq: ${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                }
                delay(3000)
            }
        }
    }

    fun updateEndpoints(points: List<InspectionPointBean>) {
        KLog.d(TAG, "外部更新导览点: count=${points.size}")
        _uiState.update { it.copy(endpoints = points) }
    }

    fun refreshMapData() {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                val points = navigationApi.getInspectionPoints()
                _uiState.update { it.copy(endpoints = points) }
                KLog.d(TAG, "手动刷新导览点成功: count=${points.size}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "手动刷新导览点失败: ${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
            }
        }
    }

    private fun loadMapImage(url: String) {
        viewModelScope.launch(Dispatchers.Default) {
            imageSeq += 1
            val seq = imageSeq
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                val isMapping = _uiState.value.isMapping
                val hasMap = _uiState.value.mapImage != null
                
                // 如果正在建图且已经有地图图片了，就不显示加载框以避免闪烁
                val showLoading = !isMapping || !hasMap
                KLog.d(TAG, "开始加载地图图片 #$seq: url=$url, isMapping=$isMapping, hasMap=$hasMap, showLoading=$showLoading")
                
                if (showLoading) {
                    _uiState.update { it.copy(isMapLoading = true, mapLoadingProgress = 0f, loadingStage = "准备下载...") }
                }

                val response = httpClient.get(url) {
                    if (showLoading) {
                        onDownload { bytesSentTotal, contentLength ->
                            if (contentLength != null && contentLength > 0) {
                                val progress = bytesSentTotal.toFloat() / contentLength
                                KLog.d(TAG, "地图图片下载进度 #$seq: ${bytesSentTotal}/${contentLength}, progress=${(progress * 100).toInt()}%")
                                _uiState.update { 
                                    it.copy(
                                        mapLoadingProgress = progress * 0.7f,
                                        loadingStage = "下载地图: ${(progress * 100).toInt()}%"
                                    )
                                }
                            }
                        }
                    }
                }
                val bytes = response.readBytes()
                KLog.d(TAG, "地图图片下载完成 #$seq: bytes=${bytes.size}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                
                if (showLoading) {
                    _uiState.update { it.copy(mapLoadingProgress = 0.8f, loadingStage = "解析地图数据...") }
                }
                
                val bitmap = PgmUtils.decodePgmToBitmap(bytes)
                if (bitmap != null) {
                    KLog.d(TAG, "地图图片解析成功 #$seq: width=${bitmap.width}, height=${bitmap.height}")
                    if (showLoading) {
                        _uiState.update { it.copy(mapLoadingProgress = 0.9f, loadingStage = "准备渲染...") }
                        delay(300) // 给予UI反馈时间
                    }
                    _uiState.update { 
                        it.copy(
                            mapImage = bitmap, 
                            mapLoadingProgress = 1.0f, 
                            loadingStage = "渲染完成",
                            isMapLoading = false
                        ) 
                    }
                    KLog.d(TAG, "地图图片加载完成 #$seq: url=$url, 总耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                } else {
                    KLog.w(TAG, "地图图片解析失败 #$seq: url=$url, bytes=${bytes.size}, 总耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                    _uiState.update { it.copy(isMapLoading = false) }
                }
            } catch (e: Exception) {
                KLog.w(TAG, "地图加载失败 #$seq: url=$url, error=${e.message}, 总耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                _uiState.update { it.copy(isMapLoading = false, loadingStage = "加载失败") }
            }
        }
    }

    // --- 交互逻辑 ---
    fun onMapLongClick(screenOffset: androidx.compose.ui.geometry.Offset, mapPos: Point2DBean) {
        KLog.d(TAG, "地图长按: screen=(${screenOffset.x}, ${screenOffset.y}), world=(${mapPos.x}, ${mapPos.y})")
        _uiState.update { it.copy(interactionMode = MapInteractionMode.SHOW_MENU, menuPosition = screenOffset, tempPoint = mapPos, selectedPoint = null) }
    }

    fun onSelectCreatePoint() {
        KLog.d(TAG, "选择地图操作: 创建导览点")
        _uiState.update { it.copy(interactionMode = MapInteractionMode.PICK_ORIENTATION) }
    }

    fun onSelectSetRobotPosition() {
        KLog.d(TAG, "选择地图操作: 设置机器人位置")
        _uiState.update { it.copy(interactionMode = MapInteractionMode.PICK_ROBOT_ORIENTATION) }
    }

    fun onSelectGoHere() {
        KLog.d(TAG, "选择地图操作: 前往此处")
        _uiState.update { it.copy(interactionMode = MapInteractionMode.PICK_GO_HERE_ORIENTATION) }
    }

    fun onMapClickForPosition(mapPos: Point2DBean) {
        KLog.d(TAG, "地图选点: world=(${mapPos.x}, ${mapPos.y})")
        _uiState.update { it.copy(tempPoint = mapPos, interactionMode = MapInteractionMode.PICK_ORIENTATION) }
    }

    fun onMapClickForOrientation(screenOffset: androidx.compose.ui.geometry.Offset, clickMapPos: Point2DBean) {
        val origin = _uiState.value.tempPoint ?: return
        val yaw = atan2(clickMapPos.y - origin.y, clickMapPos.x - origin.x)
        val nextMode = when (_uiState.value.interactionMode) {
            MapInteractionMode.PICK_ROBOT_ORIENTATION -> MapInteractionMode.CONFIRM_ROBOT_POSITION
            MapInteractionMode.PICK_GO_HERE_ORIENTATION -> MapInteractionMode.CONFIRM_GO_HERE
            else -> MapInteractionMode.CONFIRM_POINT
        }
        KLog.d(TAG, "地图选择朝向: origin=(${origin.x}, ${origin.y}), click=(${clickMapPos.x}, ${clickMapPos.y}), yaw=$yaw, nextMode=$nextMode")
        _uiState.update { it.copy(tempYaw = yaw.toFloat(), interactionMode = nextMode, menuPosition = screenOffset) }
    }

    fun onConfirmCreatePoint() {
        val state = _uiState.value
        val origin = state.tempPoint ?: return
        val finalPoint = state.selectedPoint?.copy(
            x = origin.x, y = origin.y, yaw = state.tempYaw.toDouble(),
            position = PointPosition(origin.x, origin.y, state.tempYaw.toDouble())
        ) ?: InspectionPointBean(
            name = "新点位", x = origin.x, y = origin.y, yaw = state.tempYaw.toDouble(),
            position = PointPosition(origin.x, origin.y, state.tempYaw.toDouble())
        )
        KLog.i(TAG, "确认创建/编辑导览点: pointId=${finalPoint.point_id}, name=${finalPoint.name}, x=${finalPoint.x}, y=${finalPoint.y}, yaw=${finalPoint.yaw}")
        eventBus.tryEmit(RobotEvent.RequestEditPoint(finalPoint))
        clearInteraction()
    }

    fun onConfirmSetRobotPosition() {
        val state = _uiState.value
        val origin = state.tempPoint ?: return
        val yaw = state.tempYaw.toDouble()
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始设置机器人位置: x=${origin.x}, y=${origin.y}, yaw=$yaw")
                navigationApi.setPosition(origin.x, origin.y, yaw)
                KLog.i(TAG, "设置机器人位置成功: x=${origin.x}, y=${origin.y}, yaw=$yaw, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "设置机器人位置失败: x=${origin.x}, y=${origin.y}, yaw=$yaw, error=${e.message}", e)
            }
        }
        clearInteraction()
    }

    fun onConfirmGoHere() {
        val state = _uiState.value
        val origin = state.tempPoint ?: return
        val yaw = state.tempYaw.toDouble()
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始前往此处: x=${origin.x}, y=${origin.y}, yaw=$yaw")
                navigationApi.startPoint(point_id = "", x = origin.x, y = origin.y, yaw = yaw)
                KLog.i(TAG, "前往此处成功: x=${origin.x}, y=${origin.y}, yaw=$yaw, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "前往此处失败: x=${origin.x}, y=${origin.y}, yaw=$yaw, error=${e.message}", e)
            }
        }
        clearInteraction()
    }

    fun clearInteraction() {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.NONE, menuPosition = null, selectedPoint = null, tempPoint = null, tempYaw = 0f) }
    }

    fun onPointClick(screenOffset: androidx.compose.ui.geometry.Offset, point: InspectionPointBean) {
        KLog.d(TAG, "点击导览点: pointId=${point.point_id}, name=${point.name}, screen=(${screenOffset.x}, ${screenOffset.y})")
        _uiState.update { it.copy(interactionMode = MapInteractionMode.POINT_MENU, menuPosition = screenOffset, selectedPoint = point) }
    }

    fun onGoToPoint() {
        val point = _uiState.value.selectedPoint ?: return
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始前往导览点: pointId=${point.point_id}, name=${point.name}")
                navigationApi.startPoint(point.point_id)
                KLog.i(TAG, "前往导览点成功: pointId=${point.point_id}, name=${point.name}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "前往导览点失败: pointId=${point.point_id}, name=${point.name}, error=${e.message}", e)
            }
        }
        clearInteraction()
    }

    fun onEditPoint() {
        _uiState.value.selectedPoint?.let { 
            eventBus.tryEmit(RobotEvent.RequestEditPoint(it))
        }
        clearInteraction()
    }

    fun onResetPointPosition() {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.PICK_POSITION, tempPoint = null) }
    }

    fun onDeletePoint() {
        _uiState.value.selectedPoint?.let { 
            eventBus.tryEmit(RobotEvent.RequestDeletePoint(it))
        }
        clearInteraction()
    }
}
