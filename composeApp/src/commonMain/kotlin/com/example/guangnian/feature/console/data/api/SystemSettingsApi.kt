package com.example.guangnian.feature.console.data.api

interface SystemSettingsApi {
    suspend fun getGreet(): Boolean
    suspend fun setGreet(enabled: Boolean): Boolean
    suspend fun getGreetSilentTime(): Int
    suspend fun setGreetSilentTime(time: Int): Boolean
    suspend fun getMotionAnalysis(): Boolean
    suspend fun setMotionAnalysis(enabled: Boolean): Boolean
    suspend fun getBreak(): Boolean
    suspend fun setBreak(enabled: Boolean): Boolean
}
