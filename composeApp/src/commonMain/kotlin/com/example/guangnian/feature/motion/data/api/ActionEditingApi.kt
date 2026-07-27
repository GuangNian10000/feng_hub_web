package com.example.guangnian.feature.motion.data.api

import com.example.guangnian.feature.console.data.bean.ArmActionBean
import com.example.guangnian.feature.console.data.bean.SessionResponseBean
import com.example.guangnian.feature.console.data.bean.TeachStatusBean

interface ActionEditingApi {
    suspend fun executeActionEditing(armId: String)
    suspend fun getActionEditingRecordStatus(): TeachStatusBean
    suspend fun getActionEditingList(): List<ArmActionBean>
    suspend fun setActionEditingSession(phase: String): SessionResponseBean
    suspend fun recordActionEditing(cmd: String, actionName: String? = null)
    suspend fun deleteActionEditing(name: String)
    suspend fun renameActionEditing(oldName: String, newName: String)
}