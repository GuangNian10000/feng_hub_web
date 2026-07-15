package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface NavigationApi {
    suspend fun startMapping(mapName: String)
    suspend fun endMapping()
    suspend fun getMapConfig(): MapConfigBean
    suspend fun getPosition(): RobotPositionBean
    suspend fun setPosition(x: Double, y: Double, yaw: Double)
    suspend fun startPosition(x: Double, y: Double, yaw: Double)
    suspend fun startPoint(point_id: String, x: Double? = null, y: Double? = null, yaw: Double? = null)
    suspend fun stopPosition()
    suspend fun pauseNavigation()
    suspend fun resumeNavigation()
    suspend fun getPointCloud(): List<Point2DBean>
    suspend fun getGlobalPath(): PathPointsBean
    suspend fun getLocalPath(): PathPointsBean
    suspend fun getNavStatus(): RobotNavStatusBean

    // 巡检点相关 (增删改查)
    suspend fun getInspectionPoints(): List<InspectionPointBean>
    suspend fun addInspectionPoint(point: InspectionPointBean): BaseBean // 对应 point_add (Upsert)
    suspend fun updateInspectionPoint(point: InspectionPointBean): BaseBean // 对应 point_add (Upsert)
    suspend fun deleteInspectionPoint(point_id: String): BaseBean // 对应 point_delete_by_pid
    suspend fun startInspectionPoint(point_id: String): BaseBean

    // 地图管理
    suspend fun getMaps(): MapListResponse
    suspend fun archiveMap(mapUid: String): BaseBean
}
