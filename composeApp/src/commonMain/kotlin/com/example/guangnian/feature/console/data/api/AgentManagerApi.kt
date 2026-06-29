package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface AgentManagerApi {
    suspend fun getAgentList(): AgentListResponse
    suspend fun addAgent(agent: AgentEntity): BaseBean
    suspend fun editAgent(agent: AgentEntity): BaseBean
    suspend fun deleteAgent(agent: AgentEntity): BaseBean
    suspend fun selectAgent(agent: AgentEntity): BaseBean
}
