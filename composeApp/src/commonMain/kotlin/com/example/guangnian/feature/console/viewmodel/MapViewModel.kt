package com.example.guangnian.feature.console.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.console.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.console.data.bean.*
import com.russhwolf.settings.Settings
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
    val mapImage: ImageBitmap? = null, // 需配合 Coil/Glide 将 PGM/URL 转换为 ImageBitmap
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

    init {
        startMapDataPolling()
    }

    private fun startMapDataPolling() {
        viewModelScope.launch {
            try {
                val config = robotApi.getMapConfig()
                val fixedUrl = config.getFixedImageUrl(settings.getString("saved_ip", "192.168.1.100"))
                _uiState.update { it.copy(mapConfig = config.copy(image = fixedUrl)) }
            } catch (e: Exception) {}

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