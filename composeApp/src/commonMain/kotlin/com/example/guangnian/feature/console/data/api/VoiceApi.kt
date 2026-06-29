package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface VoiceApi {
    suspend fun executeSynthesis(bean: SpeechSynthesisBean): BaseBean
}
