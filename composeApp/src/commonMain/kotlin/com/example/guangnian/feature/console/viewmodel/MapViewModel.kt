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
    val pointCloud: List<Point2DBean> = emptyList()
)

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

            // 10Hz 位置刷新
            launch {
                while (isActive) {
                    try {
                        val pos = robotApi.getPosition()
                        _uiState.update { it.copy(robotPosition = pos) }
                    } catch (e: Exception) {}
                    delay(100)
                }
            }

            // 3Hz 点云刷新
            launch {
                while (isActive) {
                    try {
                        val cloud = robotApi.getPointCloud()
                        _uiState.update { it.copy(pointCloud = cloud) }
                    } catch (e: Exception) {}
                    delay(333)
                }
            }

            // 1Hz 路径与终点刷新
            launch {
                while (isActive) {
                    try {
                        val global = robotApi.getGlobalPath()
                        val local = robotApi.getLocalPath()
                        val points = robotApi.getInspectionPoints()
                        _uiState.update {
                            it.copy(
                                globalPath = global.points,
                                localPath = local.points,
                                endpoints = points
                            )
                        }
                    } catch (e: Exception) {}
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
}
