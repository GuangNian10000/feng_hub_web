package com.example.guangnian.feature.console.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.core.utils.PgmUtils
import com.example.guangnian.feature.console.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.console.data.bean.*
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class MapUiState(
    val mapConfig: MapConfigBean? = null,
    val mapImage: ImageBitmap? = null,
    val robotPosition: RobotPositionBean? = null,
    val endpoints: List<InspectionPointBean> = emptyList(),
    val globalPath: List<Point2DBean> = emptyList(),
    val localPath: List<Point2DBean> = emptyList(),
    val pointCloud: List<Point2DBean> = emptyList(),
    // 交互相关状态
    val interactionMode: MapInteractionMode = MapInteractionMode.NONE,
    val selectedPoint: InspectionPointBean? = null, // 当前选中的导览点
    val menuPosition: androidx.compose.ui.geometry.Offset? = null, // 屏幕坐标
    val tempPoint: Point2DBean? = null, // 地图坐标
    val tempYaw: Float = 0f
)

enum class MapInteractionMode {
    NONE,
    SHOW_MENU,
    PICK_POSITION,
    PICK_ORIENTATION,
    CONFIRM_POINT,
    POINT_MENU
}

class MapViewModel : ViewModel() {
    private val robotApi = RobotTcpApiImpl()
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private val settings = Settings()
    private val httpClient = HttpClient()

    init {
        startMapDataPolling()
    }

    private fun startMapDataPolling() {
        viewModelScope.launch {
            // 独立启动地图配置获取逻辑，支持失败重试
            launch {
                while (isActive && _uiState.value.mapConfig == null) {
                    try {
                        KLog.d("MapViewModel", "正在尝试获取地图配置...")
                        val config = robotApi.getMapConfig()
                        val savedIp = settings.getString("saved_ip", "192.168.1.100")
                        val fixedUrl = config.getFixedImageUrl(savedIp)
                        
                        _uiState.update { it.copy(mapConfig = config.copy(image = fixedUrl)) }
                        KLog.d("MapViewModel", "获取配置成功，开始下载地图图片: $fixedUrl")
                        loadMapImage(fixedUrl)
                    } catch (e: Exception) {
                        KLog.e("MapViewModel", "获取地图配置失败，3秒后重试: ${e.message}")
                        delay(3000)
                    }
                }
            }

            // 5Hz 位置刷新
            launch {
                while (isActive) {
                    try {
                        val pos = robotApi.getPosition()
                        _uiState.update { it.copy(robotPosition = pos) }
                    } catch (e: Exception) {}
                    delay(200)
                }
            }

            // 1Hz 点云刷新
            launch {
                while (isActive) {
                    try {
                        val cloud = robotApi.getPointCloud()
                        _uiState.update { it.copy(pointCloud = cloud) }
                    } catch (e: Exception) {}
                    delay(1000)
                }
            }

            // 1Hz 路径刷新 (独立协程，避免失败影响其他任务)
            launch {
                while (isActive) {
                    try {
                        val global = robotApi.getGlobalPath()
                        val local = robotApi.getLocalPath()
                        _uiState.update {
                            it.copy(
                                globalPath = global.points,
                                localPath = local.points
                            )
                        }
                    } catch (e: Exception) {
                        // 路径不存在时通常会报错，静默处理
                    }
                    delay(1000)
                }
            }

            // 1Hz 导览点刷新 (独立协程，确保始终尝试更新)
            launch {
                while (isActive) {
                    try {
                        val points = robotApi.getInspectionPoints()
                        if (points.isNotEmpty() || _uiState.value.endpoints.isNotEmpty()) {
                            KLog.d("MapViewModel", "收到导览点更新: ${points.size}个")
                            _uiState.update { it.copy(endpoints = points) }
                        }
                    } catch (e: Exception) {
                        KLog.e("MapViewModel", "刷新导览点失败: ${e.message}")
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun loadMapImage(url: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                KLog.d("MapViewModel", "开始下载地图数据: $url")
                val response = httpClient.get(url)
                val bytes = response.readBytes()
                KLog.d("MapViewModel", "下载完成，字节大小: ${bytes.size}，开始解析 PGM...")
                
                val bitmap = PgmUtils.decodePgmToBitmap(bytes)
                if (bitmap != null) {
                    _uiState.update { it.copy(mapImage = bitmap) }
                    KLog.d("MapViewModel", "地图图片解析并更新成功")
                } else {
                    KLog.e("MapViewModel", "PGM 解析失败，返回 null")
                }
            } catch (e: Exception) {
                KLog.e("MapViewModel", "下载地图图片异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }

    // 坐标系转换：计算点云的绝对世界坐标
    fun calculatePointCloudWorldCoords(
        cloud: List<Point2DBean>,
        robot: RobotPositionBean
    ): List<Point2DBean> {
        val cosA = cos(robot.yaw)
        val sinA = sin(robot.yaw)
        return cloud.map { p ->
            Point2DBean(
                x = p.x * cosA - p.y * sinA + robot.x,
                y = p.x * sinA + p.y * cosA + robot.y
            )
        }
    }

    // --- 交互逻辑 ---

    fun onMapLongClick(screenOffset: androidx.compose.ui.geometry.Offset, mapPos: Point2DBean) {
        _uiState.update {
            it.copy(
                interactionMode = MapInteractionMode.SHOW_MENU,
                menuPosition = screenOffset,
                tempPoint = mapPos,
                selectedPoint = null // 长按创建新点位时，确保清除之前选中的点位状态
            )
        }
    }

    fun onSelectCreatePoint() {
        _uiState.update {
            it.copy(interactionMode = MapInteractionMode.PICK_ORIENTATION)
        }
    }

    fun onMapClickForPosition(mapPos: Point2DBean) {
        _uiState.update {
            it.copy(
                tempPoint = mapPos,
                interactionMode = MapInteractionMode.PICK_ORIENTATION
            )
        }
    }

    fun onMapClickForOrientation(screenOffset: androidx.compose.ui.geometry.Offset, clickMapPos: Point2DBean) {
        val origin = _uiState.value.tempPoint ?: return
        val dx = clickMapPos.x - origin.x
        val dy = clickMapPos.y - origin.y
        val yaw = kotlin.math.atan2(dy, dx)
        
        _uiState.update {
            it.copy(
                tempYaw = yaw.toFloat(),
                interactionMode = MapInteractionMode.CONFIRM_POINT,
                menuPosition = screenOffset // 复用 menuPosition 来定位确认框
            )
        }
    }

    fun onConfirmCreatePoint() {
        val uiState = _uiState.value
        val origin = uiState.tempPoint ?: return
        val yaw = uiState.tempYaw.toDouble()
        
        val selectedPoint = uiState.selectedPoint
        val finalPoint = if (selectedPoint != null) {
            selectedPoint.copy(
                x = origin.x,
                y = origin.y,
                yaw = yaw,
                position = PointPosition(origin.x, origin.y, yaw)
            )
        } else {
            InspectionPointBean(
                name = "新点位",
                x = origin.x,
                y = origin.y,
                yaw = yaw,
                position = PointPosition(origin.x, origin.y, yaw)
            )
        }
        _pendingPoint.value = finalPoint
        clearInteraction()
    }

    private val _pendingPoint = MutableStateFlow<InspectionPointBean?>(null)
    val pendingPoint: StateFlow<InspectionPointBean?> = _pendingPoint.asStateFlow()

    fun clearPendingPoint() {
        _pendingPoint.value = null
    }

    fun clearInteraction() {
        _uiState.update {
            it.copy(
                interactionMode = MapInteractionMode.NONE,
                menuPosition = null,
                selectedPoint = null,
                tempPoint = null,
                tempYaw = 0f
            )
        }
    }

    // 点击导览点
    fun onPointClick(screenOffset: androidx.compose.ui.geometry.Offset, point: InspectionPointBean) {
        _uiState.update {
            it.copy(
                interactionMode = MapInteractionMode.POINT_MENU,
                menuPosition = screenOffset,
                selectedPoint = point
            )
        }
    }

    // 导览点操作：修改
    fun onEditPoint() {
        val point = _uiState.value.selectedPoint ?: return
        _pendingPoint.value = point
        clearInteraction()
    }

    // 导览点操作：重设位置
    fun onResetPointPosition() {
        _uiState.update {
            it.copy(
                interactionMode = MapInteractionMode.PICK_POSITION,
                tempPoint = null
            )
        }
    }

    // 导览点操作：删除
    private val _deletePointEvent = MutableStateFlow<InspectionPointBean?>(null)
    val deletePointEvent: StateFlow<InspectionPointBean?> = _deletePointEvent.asStateFlow()

    fun onDeletePoint() {
        val point = _uiState.value.selectedPoint ?: return
        _deletePointEvent.value = point
        clearInteraction()
    }

    fun clearDeletePointEvent() {
        _deletePointEvent.value = null
    }
}
