package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

/**
 * 智能体数据模型 (对齐平板端 VoiceAgent)
 * 主要字段：人设提示词、发音人音色、所属引擎 (BASIC/INTERNATIONAL) 及元数据
 */
@Serializable
data class VoiceAgent(
    val id: String = "",
    val name: String = "", // 智能体名称（可选向后兼容）
    val prompt: String = "", // 人设提示词 (System Prompt)
    val vcn: String = "x6_lingxiaoyue_pro", // 发音人音色标识
    val voiceName: String = "聆小玥", // 音色名称
    val engine: String = "BASIC", // "BASIC" (国内基础版) 或 "INTERNATIONAL" (国际多语种版)
    val isPreset: Boolean = false, // 是否为预设智能体
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

typealias AgentEntity = VoiceAgent

@Serializable
data class AgentListResponse(
    val agent: VoiceAgent = VoiceAgent(),
    val list: List<VoiceAgent> = emptyList()
)
