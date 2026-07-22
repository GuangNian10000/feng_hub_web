package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.ArmActionBean
import com.example.guangnian.feature.console.data.bean.BatteryBean
import com.example.guangnian.core.network.tcp.BaseTcpApi
import com.example.guangnian.core.network.tcp.TcpNode
import com.example.guangnian.feature.console.data.bean.AgentEntity
import com.example.guangnian.feature.console.data.bean.AgentListResponse
import com.example.guangnian.feature.console.data.bean.HeartbeatStatusBean
import com.example.guangnian.feature.console.data.bean.InspectionPointBean
import com.example.guangnian.feature.console.data.bean.MapConfigBean
import com.example.guangnian.feature.console.data.bean.MotionBean
import com.example.guangnian.feature.console.data.bean.OfflineStateBean
import com.example.guangnian.feature.console.data.bean.PathPointsBean
import com.example.guangnian.feature.console.data.bean.Point2DBean
import com.example.guangnian.feature.console.data.bean.PostureBean
import com.example.guangnian.feature.console.data.bean.RobotPositionBean
import com.example.guangnian.feature.console.data.bean.StartupHeartbeatBean
import com.example.guangnian.feature.console.data.bean.BaseBean
import com.example.guangnian.feature.console.data.bean.SpeechSynthesisBean
import com.example.guangnian.feature.console.data.bean.TeachActionBean
import com.example.guangnian.feature.console.data.bean.TeachStatusBean
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import com.example.guangnian.feature.console.data.bean.RouteBean
import com.example.guangnian.feature.console.data.bean.RouteStatusBean

class RobotTcpApiImpl : BaseTcpApi(), RobotTcpApi {
    override suspend fun executeSynthesis(bean: SpeechSynthesisBean) =
        request<BaseBean>("/voice/tts_synthesis/", mapOf("ttsBean" to Json.encodeToString(bean)),targetNode= TcpNode.TABLET)

    override suspend fun getRoutes() = request<List<RouteBean>>("slam/route_query_all")

    override suspend fun addRoute(route: RouteBean) = request<BaseBean>("slam/route_add", mapOf(
        //"route_id" to (route.route_id ?: ""),
        "route_name" to route.route_name,
        "description" to route.description,
        "isActive" to route.isActive,
        "points" to (route.point_ids ?: emptyList()),
        "loopCount" to route.loopCount
    ))

    override suspend fun updateRoute(route: RouteBean) = request<BaseBean>("slam/route_update", mapOf(
        "route_id" to (route.route_id ?: ""),
        "route_name" to route.route_name,
        "description" to route.description,
        "isActive" to route.isActive,
        "points" to (route.point_ids ?: emptyList()),
        "loopCount" to route.loopCount
    ))

    override suspend fun deleteRoute(route_id: String) = request<BaseBean>("slam/route_delete_by_rid", mapOf(
        "route_id" to route_id
    ))

    override suspend fun startRoute(route_id: String) = request<BaseBean>("slam/start_route", mapOf(
        "route_id" to route_id
    ))

    override suspend fun getRouteStatus() = request<RouteStatusBean>("slam/get_status")

    override suspend fun selectAgent(agent: AgentEntity) =
        request<BaseBean>("agent/select_agent", mapOf("agent" to Json.encodeToString(agent)),targetNode= TcpNode.TABLET)

    override suspend fun deleteAgent(agent: AgentEntity) =
        request<BaseBean>("agent/delete_agent", mapOf("agent" to Json.encodeToString(agent)),targetNode= TcpNode.TABLET)

    override suspend fun editAgent(agent: AgentEntity) =
        request<BaseBean>("agent/edit_agent", mapOf("agent" to Json.encodeToString(agent)),targetNode= TcpNode.TABLET)

    override suspend fun addAgent(agent: AgentEntity) =
        request<BaseBean>("agent/add_agent", mapOf("agent" to Json.encodeToString(agent)),targetNode= TcpNode.TABLET)

    // 获取智能体列表
    override suspend fun getAgentList() = request<AgentListResponse>("agent/get_agent_list",targetNode= TcpNode.TABLET)

    override suspend fun getKnowledgeList() =
        request<com.example.guangnian.feature.console.data.bean.KnowledgeListResponse>("/knowledge/get_knowledge_list/", targetNode = TcpNode.TABLET)

    override suspend fun addKnowledge(knowledge: com.example.guangnian.feature.console.data.bean.KnowledgeEntity) =
        request<BaseBean>("/knowledge/add_knowledge/", mapOf("knowledge" to Json.encodeToString(knowledge)), targetNode = TcpNode.TABLET)

    override suspend fun editKnowledge(knowledge: com.example.guangnian.feature.console.data.bean.KnowledgeEntity) =
        request<BaseBean>("/knowledge/edit_knowledge/", mapOf("knowledge" to Json.encodeToString(knowledge)), targetNode = TcpNode.TABLET)

    override suspend fun deleteKnowledge(knowledge: com.example.guangnian.feature.console.data.bean.KnowledgeEntity) =
        request<BaseBean>("/knowledge/delete_knowledge/", mapOf("knowledge" to Json.encodeToString(knowledge)), targetNode = TcpNode.TABLET)

    // 获取动态动作列表
    override suspend fun getPostureList() = request<List<PostureBean>>("webrtc/get_posture")

    // 获取手臂动作列表
    override suspend fun getArmList() = request<List<ArmActionBean>>("webrtc/get_arm")

    // 获取运动控制列表
    override suspend fun getMotionList() = request<List<MotionBean>>("webrtc/get_motion")

    // 启动机器人握手
    override suspend fun startRobot() = request<StartupHeartbeatBean>("webrtc/stl_start")

    // 执行动作命令（通用）
    override suspend fun executePosture(msg: Map<String, String>) = request<Unit>("webrtc/execution", msg)

    // 执行动作命令（姿态）
    override suspend fun executePosture(postureName: String) =
        executePosture(mapOf("posture_name" to postureName))

    // 执行运动控制动作
    override suspend fun executeMotion(motionId: String) =
        request<Unit>("webrtc/execution", mapOf("motion_id" to motionId))

    // 执行预设运动控制（ahead/back/left/right/turn_left/turn_right/turn_back）
    override suspend fun executeSport(sport: String) =
        request<Unit>("webrtc/execution", mapOf("sport" to sport))

    // 执行手臂动作
    override suspend fun executeArm(armId: Int) =
        request<Unit>("webrtc/execution", mapOf("arm_id" to armId))

    // 平移摇杆控制
    override suspend fun moveTranslate(angle: Double, level: Int) =
        request<Unit>(
            "webrtc/move",
            mapOf(
                "joystick" to "translate",
                "angle" to angle,
                "level" to level
            )
        )

    // 旋转摇杆控制
    override suspend fun moveRotate(level: Int) =
        request<Unit>(
            "webrtc/move",
            mapOf(
                "joystick" to "rotate",
                "level" to level
            )
        )

    // 混合摇杆控制
    override suspend fun moveMixed(angle: Double, translateLevel: Int, rotateLevel: Int) =
        request<Unit>(
            "webrtc/move",
            mapOf(
                "joystick" to "translate",
                "angle" to angle,
                "level" to translateLevel,
                "rotatelevel" to rotateLevel
            )
        )

    // 停止平移摇杆
    override suspend fun stopTranslate() =
        request<Unit>("webrtc/stop", mapOf("joystick" to "translate"))

    // 停止旋转摇杆
    override suspend fun stopRotate() =
        request<Unit>("webrtc/stop", mapOf("joystick" to "rotate"))

    // 上报 TCP 断开状态
    override suspend fun notifyTcpDisconnected() =
        request<Unit>("webrtc/state", mapOf("state" to "false"))

    // 发送心跳包
    override suspend fun sendHeartbeat(timestamp: Long) =
        request<HeartbeatStatusBean>(
            "heartbeat/connection",
            mapOf("timestamp" to timestamp.toString())
        )

    // 开始录制示教动作
    override suspend fun startTeach(name: String) =
        request<Unit>("teach/start", mapOf("name" to name))

    // 结束录制示教动作
    override suspend fun stopTeach() = request<Unit>("teach/stop")

    // 删除示教动作
    override suspend fun deleteTeach(name: String) =
        request<Unit>("teach/delete", mapOf("name" to name))

    // 重命名示教动作
    override suspend fun renameTeach(oldName: String, newName: String) =
        request<Unit>(
            "teach/rename",
            mapOf(
                "old_name" to oldName,
                "new_name" to newName
            )
        )

    // 获取示教动作列表
    override suspend fun getTeachActionList() =
        request<List<TeachActionBean>>("teach/get_action")

    // 执行示教动作
    override suspend fun executeTeach(name: String) =
        request<Unit>("teach/execution", mapOf("name" to name))

    override suspend fun getTeachStatus() =
        request<TeachStatusBean>("teach/record_status")

    override suspend fun setAesKey(key: String) =
        request<Unit>("webrtc/aes_set", mapOf("aes_key" to key))

    // 开始建图（协议约定字段名为 msg）
    override suspend fun startMapping(mapName: String) =
        request<Unit>("slam/start_mapping", mapOf("msg" to mapName))

    // 结束建图
    override suspend fun endMapping() = request<Unit>("slam/end_mapping")

    // 获取地图配置
    override suspend fun getMapConfig() = request<MapConfigBean>("slam/map_config")

    // 获取当前机器人坐标
    override suspend fun getPosition() = request<RobotPositionBean>("slam/get_position")

    // 设置机器人坐标（重定位）
    override suspend fun setPosition(x: Double, y: Double, yaw: Double) =
        request<Unit>(
            "slam/set_position",
            mapOf(
                "x" to x.toString(),
                "y" to y.toString(),
                "yaw" to yaw.toString()
            )
        )

    // 前往某坐标点
    override suspend fun startPosition(x: Double, y: Double, yaw: Double) =
        request<Unit>(
            "slam/start_position",
            mapOf(
                "x" to x.toString(),
                "y" to y.toString(),
                "yaw" to yaw.toString()
            )
        )

    // 前往某标记点（坐标参数可选）
    override suspend fun startPoint(point_id: String, x: Double?, y: Double?, yaw: Double?) {
        val msg = mutableMapOf("point_id" to point_id)
        x?.let { msg["x"] = it.toString() }
        y?.let { msg["y"] = it.toString() }
        yaw?.let { msg["yaw"] = it.toString() }
        request<Unit>("slam/start_point", msg)
    }

    // 前往导航点
    override suspend fun startInspectionPoint(point_id: String) =
        request<BaseBean>("slam/start_point", mapOf("point_id" to point_id))

    // 停止当前导航目标
    override suspend fun stopPosition() = request<Unit>("slam/stop_position")

    // 暂停导览
    override suspend fun pauseNavigation() = request<BaseBean>("slam/pause")

    // 继续导览
    override suspend fun resumeNavigation() = request<BaseBean>("slam/resume")

    // 获取 2D 点云数据
    override suspend fun getPointCloud() = request<List<Point2DBean>>("slam/point_cloud")

    // 急停（停止运动/建图）
    override suspend fun stopAll() = request<Unit>("slam/stop_all")

    // 恢复急停
    override suspend fun recover() = request<Unit>("slam/recover")

    // 查询急停状态
    override suspend fun getEmergency() = request<Unit>("slam/get_emergency")

    // 获取导航点列表
    override suspend fun getInspectionPoints() =
        request<List<InspectionPointBean>>("slam/point_query_all")

    // 获取地图列表
    override suspend fun getMaps() = request<com.example.guangnian.feature.console.data.bean.MapListResponse>("map/list")

    // 归档（删除）地图
    override suspend fun archiveMap(mapUid: String) = request<BaseBean>("map/archive",mapOf("map_uid" to mapUid))//("map", mapOf("topic" to "archive", "map_uid" to mapUid))

    // 切换地图
    override suspend fun switchMap(map_uid: String) = request<BaseBean>("map/switch",
            mapOf(
                "map_uid" to map_uid
            ))

    // 创建导航点
    override suspend fun addInspectionPoint(point: InspectionPointBean) =
        request<BaseBean>("slam/point_add", mapOf(
            "point_name" to point.name,
            "x" to (point.position?.x ?: point.x),
            "y" to (point.position?.y ?: point.y),
            "yaw" to (point.position?.yaw ?: point.yaw),
            "description" to point.broadcastContent,
            "play_on_arrival" to point.playOnArrival,
            "photo_on_arrival" to point.photoOnArrival,
            "point_type" to point.pointType,
            "stay_time" to point.stayTime,
            "need_rotate" to point.needRotate,
            "arm_id" to (point.armId ?: ""),
            "skip_allowed" to point.skipAllowed,
            "play_priority" to point.playPriority
        ))

    // 更新导航点
    override suspend fun updateInspectionPoint(point: InspectionPointBean) =
        request<BaseBean>("slam/point_update", mapOf(
            "point_id" to point.point_id,
            "point_name" to point.name,
            "x" to (point.position?.x ?: point.x),
            "y" to (point.position?.y ?: point.y),
            "yaw" to (point.position?.yaw ?: point.yaw),
            "description" to point.broadcastContent,
            "play_on_arrival" to point.playOnArrival,
            "photo_on_arrival" to point.photoOnArrival,
            "point_type" to point.pointType,
            "stay_time" to point.stayTime,
            "need_rotate" to point.needRotate,
            "arm_id" to (point.armId ?: ""),
            "skip_allowed" to point.skipAllowed,
            "play_priority" to point.playPriority
        ))

    // 删除导航点
    override suspend fun deleteInspectionPoint(point_id: String) =
        request<BaseBean>("slam/point_delete_by_pid", mapOf("point_id" to point_id))

    // 获取全局路径规划
    override suspend fun getGlobalPath() = request<PathPointsBean>("slam/get_global_path")

    // 获取局部路径规划
    override suspend fun getLocalPath() = request<PathPointsBean>("slam/get_local_path")

    override suspend fun getNavStatus() = request<com.example.guangnian.feature.console.data.bean.RobotNavStatusBean>("slam/get_status")

    // --- FaceApi 实现 ---
    override suspend fun robotCaptureFace(name: String) =
        request<Unit>("face/add_person", mapOf("name" to name), targetNode = TcpNode.TABLET)

    override suspend fun deleteFace(name: String) =
        request<BaseBean>("face/delete_person", mapOf("name" to name), targetNode = TcpNode.TABLET)

    // 获取机器人电量
    override suspend fun getBattery() = request<BatteryBean>("state/get_battery")

    // 发送文本播报请求（state/tts）
    override suspend fun sendTts(text: String) =
        request<Unit>("state/tts", mapOf("text" to text))

    // 回执文本播报已处理（state/tts）
    override suspend fun replyTtsReceived() =
        request<Unit>("state/tts", mapOf("receive" to "true"))

    // 获取离线状态摘要
    override suspend fun getOfflineState() = request<OfflineStateBean>("offline/get_state")
}
