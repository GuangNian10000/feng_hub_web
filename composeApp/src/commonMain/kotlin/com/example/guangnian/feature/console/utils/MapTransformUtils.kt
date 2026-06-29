package com.example.guangnian.feature.console.utils

import androidx.compose.ui.geometry.Offset
import com.example.guangnian.feature.console.data.bean.MapConfigBean
import com.example.guangnian.feature.console.data.bean.Point2DBean
import com.example.guangnian.feature.console.data.bean.RobotPositionBean
import kotlin.math.cos
import kotlin.math.sin

object MapTransformUtils {

    /**
     * 计算点云的世界坐标
     */
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

    /**
     * 世界坐标转地图像素坐标 (处理 Y 轴翻转)
     * ROS: Y向上为正 | Image: Y向下为正
     */
    fun worldToPixel(
        worldX: Double,
        worldY: Double,
        config: MapConfigBean,
        mapHeightPx: Float
    ): Offset {
        val originX = config.origin.getOrNull(0) ?: 0.0
        val originY = config.origin.getOrNull(1) ?: 0.0
        val res = if (config.resolution > 0) config.resolution else 0.05
        
        val px = (worldX - originX) / res
        val py = mapHeightPx - (worldY - originY) / res
        return Offset(px.toFloat(), py.toFloat())
    }

    /**
     * 地图像素坐标转世界坐标
     */
    fun pixelToWorld(
        pixel: Offset,
        config: MapConfigBean,
        mapHeightPx: Float
    ): Point2DBean {
        val originX = config.origin.getOrNull(0) ?: 0.0
        val originY = config.origin.getOrNull(1) ?: 0.0
        val res = if (config.resolution > 0) config.resolution else 0.05
        
        val worldX = pixel.x.toDouble() * res + originX
        val worldY = (mapHeightPx - pixel.y).toDouble() * res + originY
        return Point2DBean(worldX, worldY)
    }
}
