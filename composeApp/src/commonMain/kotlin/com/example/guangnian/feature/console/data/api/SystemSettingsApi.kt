package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.BaseBean

interface SystemSettingsApi {
    suspend fun getGreet(): Boolean
    suspend fun setGreet(enabled: Boolean): BaseBean
    suspend fun getGreetSilentTime(): Int
    suspend fun setGreetSilentTime(time: Int): BaseBean
    suspend fun getMotionAnalysis(): Boolean
    suspend fun setMotionAnalysis(enabled: Boolean): BaseBean
    suspend fun getBreak(): Boolean
    suspend fun setBreak(enabled: Boolean): BaseBean
}
