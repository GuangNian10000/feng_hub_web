package com.example.guangnian.feature.robot.data.api

import com.example.guangnian.core.network.tcp.BaseTcpApi

class RobotTcpApiImpl  : BaseTcpApi(), RobotTcpApi {

    // 获取电池信息
    override suspend fun getBatteryInfo() = request<BatteryBean>("state/get_system_info")

    // 执行动作命令
    override suspend fun executePosture(msg: Map<String, String>) = request<Unit>("webrtc/execution", msg)
}