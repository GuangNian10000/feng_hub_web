package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface NavigationApi {
    suspend fun startMapping(mapName: String)
    suspend fun endMapping()
    suspend fun getMapConfig(): MapConfigBean
    suspend fun getPosition(): RobotPositionBean
    suspend fun setPosition(x: Double, y: Double, yaw: Double)
    suspend fun startPosition(x: Double, y: Double, yaw: Double)
    suspend fun startPoint(pointId: String, x: Double? = null, y: Double? = null, yaw: Double? = null)
    suspend fun stopPosition()
    suspend fun getPointCloud(): List<Point2DBean>
    suspend fun getGlobalPath(): PathPointsBean
    suspend fun getLocalPath(): PathPointsBean

    // 巡检点相关
    suspend fun getInspectionPoints(): List<InspectionPointBean>
    suspend fun addInspectionPoint(point: InspectionPointBean): BaseBean
    suspend fun updateInspectionPoint(point: InspectionPointBean): BaseBean
    suspend fun deleteInspectionPoint(pointId: String): BaseBean
    suspend fun startInspectionPoint(pointId: String): BaseBean
}
