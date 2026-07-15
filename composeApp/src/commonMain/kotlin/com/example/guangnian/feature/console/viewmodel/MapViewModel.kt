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
import kotlinx.coroutines.launch
import kotlin.math.atan2

data class MapUiState(
    val mapConfig: MapConfigBean? = null,
    val mapImage: ImageBitmap? = null,
    val robotPosition: RobotPositionBean? = null,
    val endpoints: List<InspectionPointBean> = emptyList(),
    val globalPath: List<Point2DBean> = emptyList(),
    val localPath: List<Point2DBean> = emptyList(),
    val pointCloud: List<Point2DBean> = emptyList(),
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
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        startMapDataPolling()
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is RobotEvent.PointsChanged -> {
                        refreshMapData()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startMapDataPolling() {
        // 1. 地图配置与图片加载
        viewModelScope.launch {
            while (isActive && _uiState.value.mapConfig == null) {
                try {
                    val config = navigationApi.getMapConfig()
                    val savedIp = settings.getString("saved_ip", "192.168.1.100")
                    val fixedUrl = config.getFixedImageUrl(savedIp)
                    _uiState.update { it.copy(mapConfig = config.copy(image = fixedUrl)) }
                    loadMapImage(fixedUrl)
                } catch (e: Exception) {
                    delay(3000)
                }
            }
        }

        // 2. 高频位置刷新 (5Hz)
        viewModelScope.launch {
            while (isActive) {
                try {
                    val pos = navigationApi.getPosition()
                    _uiState.update { it.copy(robotPosition = pos) }
                } catch (e: Exception) {
                    delay(3000) //500
                }
                delay(3000) //200
            }
        }

        // 3. 动效数据刷新 (1Hz: 点云、路径)
        viewModelScope.launch {
            while (isActive) {
                try {
                    coroutineScope {
                        val cloudDef = async { try { navigationApi.getPointCloud() } catch(e: Exception) { emptyList<Point2DBean>() } }
                        val globalDef = async { try { navigationApi.getGlobalPath().points } catch(e: Exception) { emptyList<Point2DBean>() } }
                        val localDef = async { try { navigationApi.getLocalPath().points } catch(e: Exception) { emptyList<Point2DBean>() } }
                        
                        _uiState.update { it.copy(
                            pointCloud = cloudDef.await(),
                            globalPath = globalDef.await(),
                            localPath = localDef.await()
                        ) }
                    }
                } catch (e: Exception) {
                    KLog.e("MapViewModel", "路径数据同步异常: ${e.message}")
                }
                delay(3000)
            }
        }

        // 4. 导览点数据轮询 (1Hz)
        viewModelScope.launch {
            while (isActive) {
                try {
                    val points = navigationApi.getInspectionPoints()
                    _uiState.update { it.copy(endpoints = points) }
                } catch (e: Exception) {
                    KLog.e("MapViewModel", "导览点同步异常")
                }
                delay(3000)
            }
        }
    }

    fun updateEndpoints(points: List<InspectionPointBean>) {
        _uiState.update { it.copy(endpoints = points) }
    }

    fun refreshMapData() {
        viewModelScope.launch {
            try {
                val points = navigationApi.getInspectionPoints()
                _uiState.update { it.copy(endpoints = points) }
            } catch (e: Exception) {
                KLog.e("MapViewModel", "刷新导览点失败")
            }
        }
    }

    private fun loadMapImage(url: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _uiState.update { it.copy(isMapLoading = true, mapLoadingProgress = 0f, loadingStage = "准备下载...") }
                val response = httpClient.get(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            val progress = bytesSentTotal.toFloat() / contentLength
                            _uiState.update { 
                                it.copy(
                                    mapLoadingProgress = progress * 0.7f,
                                    loadingStage = "下载地图: ${(progress * 100).toInt()}%"
                                )
                            }
                        }
                    }
                }
                val bytes = response.readBytes()
                _uiState.update { it.copy(mapLoadingProgress = 0.8f, loadingStage = "解析地图数据...") }
                
                val bitmap = PgmUtils.decodePgmToBitmap(bytes)
                if (bitmap != null) {
                    _uiState.update { it.copy(mapLoadingProgress = 0.9f, loadingStage = "准备渲染...") }
                    delay(300) // 给予UI反馈时间
                    _uiState.update { 
                        it.copy(
                            mapImage = bitmap, 
                            mapLoadingProgress = 1.0f, 
                            loadingStage = "渲染完成",
                            isMapLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                KLog.e("MapViewModel", "地图加载失败: ${e.message}")
                _uiState.update { it.copy(isMapLoading = false, loadingStage = "加载失败") }
            }
        }
    }

    // --- 交互逻辑 ---
    fun onMapLongClick(screenOffset: androidx.compose.ui.geometry.Offset, mapPos: Point2DBean) {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.SHOW_MENU, menuPosition = screenOffset, tempPoint = mapPos, selectedPoint = null) }
    }

    fun onSelectCreatePoint() {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.PICK_ORIENTATION) }
    }

    fun onSelectSetRobotPosition() {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.PICK_ROBOT_ORIENTATION) }
    }

    fun onSelectGoHere() {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.PICK_GO_HERE_ORIENTATION) }
    }

    fun onMapClickForPosition(mapPos: Point2DBean) {
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
        eventBus.tryEmit(RobotEvent.RequestEditPoint(finalPoint))
        clearInteraction()
    }

    fun onConfirmSetRobotPosition() {
        val state = _uiState.value
        val origin = state.tempPoint ?: return
        val yaw = state.tempYaw.toDouble()
        viewModelScope.launch {
            try {
                navigationApi.setPosition(origin.x, origin.y, yaw)
                KLog.d("MapViewModel", "设置机器人位置成功: x=${origin.x}, y=${origin.y}, yaw=$yaw")
            } catch (e: Exception) {
                KLog.e("MapViewModel", "设置机器人位置失败: ${e.message}")
            }
        }
        clearInteraction()
    }

    fun onConfirmGoHere() {
        val state = _uiState.value
        val origin = state.tempPoint ?: return
        val yaw = state.tempYaw.toDouble()
        viewModelScope.launch {
            try {
                navigationApi.startPoint(point_id = "", x = origin.x, y = origin.y, yaw = yaw)
                KLog.d("MapViewModel", "前往此处成功: x=${origin.x}, y=${origin.y}, yaw=$yaw")
            } catch (e: Exception) {
                KLog.e("MapViewModel", "前往此处失败: ${e.message}")
            }
        }
        clearInteraction()
    }

    fun clearInteraction() {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.NONE, menuPosition = null, selectedPoint = null, tempPoint = null, tempYaw = 0f) }
    }

    fun onPointClick(screenOffset: androidx.compose.ui.geometry.Offset, point: InspectionPointBean) {
        _uiState.update { it.copy(interactionMode = MapInteractionMode.POINT_MENU, menuPosition = screenOffset, selectedPoint = point) }
    }

    fun onGoToPoint() {
        val point = _uiState.value.selectedPoint ?: return
        viewModelScope.launch {
            try {
                navigationApi.startPoint(point.point_id)
            } catch (e: Exception) {
                KLog.e("MapViewModel", "Failed to go to point: ${e.message}")
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
