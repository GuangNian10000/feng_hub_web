package com.example.guangnian.core.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

data class LogMessage(
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String
)

object LogCollector {
    private val _logs = MutableStateFlow<List<LogMessage>>(emptyList())
    val logs = _logs.asStateFlow()

    fun addLog(level: String, tag: String, message: String) {
        val currentList = _logs.value.toMutableList()
        currentList.add(LogMessage(Clock.System.now().toEpochMilliseconds(), level, tag, message))
        if (currentList.size > 1000) {
            currentList.removeAt(0)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
