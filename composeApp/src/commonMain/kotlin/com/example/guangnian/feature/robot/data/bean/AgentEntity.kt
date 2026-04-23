package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class AgentListResponse(val agent: AgentEntity = AgentEntity(), val list: List<AgentEntity> = emptyList())

@Serializable
class AgentEntity {
    var id: Long = 0
    var name: String = "小峰" //名称
    var promptWord: String = "" //提示词
    var voice: VoiceBean = VoiceBean() //音色
    var createTime: Long = Clock.System.now().toEpochMilliseconds() //创建时间
    var updateTime: Long = Clock.System.now().toEpochMilliseconds() //更新时间
}