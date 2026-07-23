package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

@Serializable
data class KnowledgeListResponse(val list: List<KnowledgeEntity> = emptyList())

@Serializable
class KnowledgeEntity {
    var id: Long = 0
    var question: String = "" // 问题
    var answer: String = "" // 答案
    var createTime: Long = Clock.System.now().toEpochMilliseconds() // 创建时间
    var updateTime: Long = Clock.System.now().toEpochMilliseconds() // 更新时间
}
