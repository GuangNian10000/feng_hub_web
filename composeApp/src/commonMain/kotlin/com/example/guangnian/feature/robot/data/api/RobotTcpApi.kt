package com.example.guangnian.feature.robot.data.api

import com.example.guangnian.feature.robot.data.bean.ArmActionBean
import com.example.guangnian.feature.robot.data.bean.BatteryBean
import com.example.guangnian.feature.robot.data.bean.HeartbeatStatusBean
import com.example.guangnian.feature.robot.data.bean.MapConfigBean
import com.example.guangnian.feature.robot.data.bean.MotionBean
import com.example.guangnian.feature.robot.data.bean.OfflineStateBean
import com.example.guangnian.feature.robot.data.bean.PathPointsBean
import com.example.guangnian.feature.robot.data.bean.Point2DBean
import com.example.guangnian.feature.robot.data.bean.PostureBean
import com.example.guangnian.feature.robot.data.bean.RobotPositionBean
import com.example.guangnian.feature.robot.data.bean.SlamPointBean
import com.example.guangnian.feature.robot.data.bean.StartupHeartbeatBean
import com.example.guangnian.feature.robot.data.bean.TeachActionBean

interface RobotTcpApi {
    // 启动机器人握手 (webrtc/stl_start)
    suspend fun startRobot(): StartupHeartbeatBean

    // 执行动作命令（通用 execution；兼容旧调用）
    suspend fun executePosture(msg: Map<String, String>)

    // 执行动作命令（posture_name）
    suspend fun executePosture(postureName: String)

    // 获取姿态动作列表 (webrtc/get_posture)
    suspend fun getPostureList(): List<PostureBean>

    // 获取运动控制列表 (webrtc/get_motion)
    suspend fun getMotionList(): List<MotionBean>

    // 执行运动控制动作（motion_id）
    suspend fun executeMotion(motionId: Int)

    // 执行预设运动控制（sport）
    suspend fun executeSport(sport: String)

    // 获取手臂动作列表 (webrtc/get_arm)
    suspend fun getArmList(): List<ArmActionBean>

    // 执行手臂动作（arm_id）
    suspend fun executeArm(armId: Int)

    // 摇杆平移控制 (webrtc/move, joystick=translate)
    suspend fun moveTranslate(angle: Double, level: Int)

    // 摇杆旋转控制 (webrtc/move, joystick=rotate)
    suspend fun moveRotate(level: Int)

    // 停止平移摇杆
    suspend fun stopTranslate()

    // 停止旋转摇杆
    suspend fun stopRotate()

    // 上报 TCP 断开状态
    suspend fun notifyTcpDisconnected()

    // 发送心跳包 (heartbeat/connection)
    suspend fun sendHeartbeat(timestamp: Long): HeartbeatStatusBean

    // 开始录制示教动作
    suspend fun startTeach(name: String)

    // 结束录制示教动作
    suspend fun stopTeach()

    // 删除示教动作
    suspend fun deleteTeach(name: String)

    // 重命名示教动作
    suspend fun renameTeach(oldName: String, newName: String)

    // 获取示教动作列表
    suspend fun getTeachActionList(): List<TeachActionBean>

    // 执行示教动作
    suspend fun executeTeach(name: String)

    // 开始建图
    suspend fun startMapping(mapName: String)

    // 结束建图
    suspend fun endMapping()

    // 获取地图配置
    suspend fun getMapConfig(): MapConfigBean

    // 获取当前机器人坐标
    suspend fun getPosition(): RobotPositionBean

    // 设置机器人坐标（重定位）
    suspend fun setPosition(x: Double, y: Double, yaw: Double)

    // 前往某坐标点
    suspend fun startPosition(x: Double, y: Double, yaw: Double)

    // 前往某标记点（坐标参数可选）
    suspend fun startPoint(pointId: String, x: Double? = null, y: Double? = null, yaw: Double? = null)

    // 停止当前导航目标
    suspend fun stopPosition()

    // 获取 2D 点云数据
    suspend fun getPointCloud(): List<Point2DBean>

    // 急停（停止运动/建图）
    suspend fun stopAll()

    // 获取点位列表
    suspend fun getPointList(): List<SlamPointBean>

    // 获取全局路径规划
    suspend fun getGlobalPath(): PathPointsBean

    // 获取局部路径规划
    suspend fun getLocalPath(): PathPointsBean

    // 获取机器人电量
    suspend fun getBattery(): BatteryBean

    // 发送文本播报请求（state/tts）
    suspend fun sendTts(text: String)

    // 回执文本播报已处理（state/tts）
    suspend fun replyTtsReceived()

    // 获取离线状态摘要
    suspend fun getOfflineState(): OfflineStateBean
}
