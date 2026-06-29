package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface KnowledgeApi {
    suspend fun getKnowledgeList(): KnowledgeListResponse
    suspend fun addKnowledge(knowledge: KnowledgeEntity): BaseBean
    suspend fun editKnowledge(knowledge: KnowledgeEntity): BaseBean
    suspend fun deleteKnowledge(knowledge: KnowledgeEntity): BaseBean
}
