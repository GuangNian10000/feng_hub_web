package com.example.guangnian.feature.console.utils

import com.example.guangnian.feature.console.data.bean.MapConfigBean
import com.example.guangnian.feature.console.data.bean.Point2DBean
import com.example.guangnian.feature.console.data.bean.RobotPositionBean
import kotlin.math.cos
import kotlin.math.sin

object MapTransformUtils {

    /**
     * 计算点云的世界坐标
     * 将点云（相对于机器人的局部坐标）转换为相对于地图原点的世界坐标
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
     * 世界坐标转地图像素坐标
     * @param worldX 世界坐标 X (meters)
     * @param worldY 世界坐标 Y (meters)
     * @param config 地图配置（包含分辨率和原点）
     */
    fun worldToPixel(
        worldX: Double,
        worldY: Double,
        config: MapConfigBean
    ): Pair<Float, Float> {
        val originX = config.origin.getOrNull(0) ?: 0.0
        val originY = config.origin.getOrNull(1) ?: 0.0
        
        val pixelX = (worldX - originX) / config.resolution
        // 注意：通常 ROS 地图 Y 轴是向上的，而图片坐标系 Y 轴是向下的。
        // 但这里我们先提供基础数学转换，UI 层的翻转在绘图逻辑中处理。
        val pixelY = (worldY - originY) / config.resolution
        return pixelX.toFloat() to pixelY.toFloat()
    }

    /**
     * 地图像素坐标转世界坐标
     */
    fun pixelToWorld(
        pixelX: Float,
        pixelY: Float,
        config: MapConfigBean
    ): Point2DBean {
        val originX = config.origin.getOrNull(0) ?: 0.0
        val originY = config.origin.getOrNull(1) ?: 0.0
        
        return Point2DBean(
            x = pixelX.toDouble() * config.resolution + originX,
            y = pixelY.toDouble() * config.resolution + originY
        )
    }
}
