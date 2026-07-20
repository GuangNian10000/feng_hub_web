package com.example.guangnian.feature.teach.data.api

import com.example.guangnian.feature.console.data.bean.TeachActionBean
import com.example.guangnian.feature.console.data.bean.TeachStatusBean

interface TeachApi {
    suspend fun startTeach(name: String)
    suspend fun stopTeach()
    suspend fun deleteTeach(name: String)
    suspend fun renameTeach(oldName: String, newName: String)
    suspend fun getTeachActionList(): List<TeachActionBean>
    suspend fun executeTeach(name: String)
    suspend fun getTeachStatus(): TeachStatusBean
}