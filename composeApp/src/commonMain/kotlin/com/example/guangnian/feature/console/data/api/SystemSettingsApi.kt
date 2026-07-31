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
    suspend fun getBootId(): String
    suspend fun setBootId(bootId: String): BaseBean
    suspend fun getInterruptWords(): List<String>
    suspend fun setInterruptWords(words: List<String>): BaseBean
    suspend fun getWakeUpWords(): List<String>
    suspend fun setWakeUpWords(words: List<String>): BaseBean
    suspend fun getSleepWords(): List<String>
    suspend fun setSleepWords(words: List<String>): BaseBean
    suspend fun getNavigationWords(): List<String>
    suspend fun setNavigationWords(words: List<String>): BaseBean
    suspend fun getVisionWords(): List<String>
    suspend fun setVisionWords(words: List<String>): BaseBean
    suspend fun getCarouselImageDuration(): Int
    suspend fun setCarouselImageDuration(duration: Int): BaseBean
}
