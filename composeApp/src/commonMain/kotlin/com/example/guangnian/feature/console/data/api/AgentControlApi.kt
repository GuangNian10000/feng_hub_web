package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.AgentStatusBean

interface AgentControlApi {
    suspend fun getAgentStatus(): AgentStatusBean
    suspend fun setAgentStatus(status: AgentStatusBean)
    suspend fun interruptDialogue()
    suspend fun setVolume(increase: Boolean)
}
