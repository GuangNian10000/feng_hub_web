package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface TeachApi {
    suspend fun startTeach(name: String)
    suspend fun stopTeach()
    suspend fun deleteTeach(name: String)
    suspend fun renameTeach(oldName: String, newName: String)
    suspend fun getTeachActionList(): List<TeachActionBean>
    suspend fun executeTeach(name: String)
}
