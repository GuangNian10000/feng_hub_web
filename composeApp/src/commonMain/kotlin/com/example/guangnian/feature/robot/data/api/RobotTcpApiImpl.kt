package com.example.guangnian.feature.robot.data.api

import com.example.guangnian.feature.robot.data.bean.ArmActionBean
import com.example.guangnian.feature.robot.data.bean.BatteryBean
import com.example.guangnian.core.network.tcp.BaseTcpApi
import com.example.guangnian.core.network.tcp.TcpNode
import com.example.guangnian.feature.robot.data.bean.AgentEntity
import com.example.guangnian.feature.robot.data.bean.AgentListResponse
import com.example.guangnian.feature.robot.data.bean.HeartbeatStatusBean
import com.example.guangnian.feature.robot.data.bean.InspectionPointBean
import com.example.guangnian.feature.robot.data.bean.MapConfigBean
import com.example.guangnian.feature.robot.data.bean.MotionBean
import com.example.guangnian.feature.robot.data.bean.OfflineStateBean
import com.example.guangnian.feature.robot.data.bean.PathPointsBean
import com.example.guangnian.feature.robot.data.bean.Point2DBean
import com.example.guangnian.feature.robot.data.bean.PostureBean
import com.example.guangnian.feature.robot.data.bean.RobotPositionBean
import com.example.guangnian.feature.robot.data.bean.StartupHeartbeatBean
import com.example.guangnian.feature.robot.data.bean.BaseBean
import com.example.guangnian.feature.robot.data.bean.SpeechSynthesisBean
import com.example.guangnian.feature.robot.data.bean.TeachActionBean
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RobotTcpApiImpl : BaseTcpApi(), RobotTcpApi {
    override suspend fun executeSynthesis(bean: SpeechSynthesisBean) =
        request<BaseBean>("/voice/tts_synthesis/", mapOf("ttsBean" to Json.encodeToString(bean)),targetNode= TcpNode.TABLET)

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
        request<com.example.guangnian.feature.robot.data.bean.KnowledgeListResponse>("/knowledge/get_knowledge_list/", targetNode = TcpNode.TABLET)

    override suspend fun addKnowledge(knowledge: com.example.guangnian.feature.robot.data.bean.KnowledgeEntity) =
        request<BaseBean>("/knowledge/add_knowledge/", mapOf("knowledge" to Json.encodeToString(knowledge)), targetNode = TcpNode.TABLET)

    override suspend fun editKnowledge(knowledge: com.example.guangnian.feature.robot.data.bean.KnowledgeEntity) =
        request<BaseBean>("/knowledge/edit_knowledge/", mapOf("knowledge" to Json.encodeToString(knowledge)), targetNode = TcpNode.TABLET)

    override suspend fun deleteKnowledge(knowledge: com.example.guangnian.feature.robot.data.bean.KnowledgeEntity) =
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
    override suspend fun executeMotion(motionId: Int) =
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
    override suspend fun startPoint(pointId: String, x: Double?, y: Double?, yaw: Double?) {
        val msg = mutableMapOf("point_id" to pointId)
        x?.let { msg["x"] = it.toString() }
        y?.let { msg["y"] = it.toString() }
        yaw?.let { msg["yaw"] = it.toString() }
        request<Unit>("slam/start_point", msg)
    }

    // 前往巡检点
    override suspend fun startInspectionPoint(pointId: String) =
        request<BaseBean>("location/start_point", mapOf("pointId" to pointId))

    // 停止当前导航目标
    override suspend fun stopPosition() = request<Unit>("slam/stop_position")

    // 获取 2D 点云数据
    override suspend fun getPointCloud() = request<List<Point2DBean>>("slam/point_cloud")

    // 急停（停止运动/建图）
    override suspend fun stopAll() = request<Unit>("slam/stop_all")

    // 获取巡检点列表
    override suspend fun getInspectionPoints() =
        request<List<InspectionPointBean>>("location/get_endpoints")

    // 创建巡检点
    override suspend fun addInspectionPoint(point: InspectionPointBean) =
        request<BaseBean>("location/add_endpoint", mapOf(
            "name" to point.name,
            "broadcastContent" to point.broadcastContent,
            "playOnArrival" to point.playOnArrival,
            "photoOnArrival" to point.photoOnArrival,
            "pointType" to point.pointType,
            "stayTime" to point.stayTime,
            "needRotate" to point.needRotate,
            "position" to mapOf(
                "x" to (point.position?.x ?: point.x),
                "y" to (point.position?.y ?: point.y),
                "yaw" to (point.position?.yaw ?: point.yaw)
            )
        ))

    // 更新巡检点
    override suspend fun updateInspectionPoint(point: InspectionPointBean) =
        request<BaseBean>("location/update_endpoint", mapOf(
            "pointId" to point.pointId,
            "name" to point.name,
            "broadcastContent" to point.broadcastContent,
            "playOnArrival" to point.playOnArrival,
            "photoOnArrival" to point.photoOnArrival,
            "pointType" to point.pointType,
            "stayTime" to point.stayTime,
            "needRotate" to point.needRotate,
            "position" to mapOf(
                "x" to (point.position?.x ?: point.x),
                "y" to (point.position?.y ?: point.y),
                "yaw" to (point.position?.yaw ?: point.yaw)
            )
        ))

    // 删除巡检点
    override suspend fun deleteInspectionPoint(pointId: String) =
        request<BaseBean>("location/delete_endpoint", mapOf("pointId" to pointId))

    // 获取全局路径规划
    override suspend fun getGlobalPath() = request<PathPointsBean>("slam/get_global_path")

    // 获取局部路径规划
    override suspend fun getLocalPath() = request<PathPointsBean>("slam/get_local_path")

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
