package com.example.guangnian.core.network.ws

import com.example.guangnian.core.utils.KLog
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 手机端全局通讯框架管理器
 * 负责持久化存储与管理当前所选框架（新框架 TCP直连 / 老框架 WS连接）
 */
class FrameworkManager(private val settings: Settings) {
    companion object {
        private const val TAG = "FrameworkManager"
        const val KEY_FRAMEWORK = "communication_framework"

        var instance: FrameworkManager? = null
            private set
    }

    init {
        instance = this
    }

    private val _currentFramework = MutableStateFlow(loadInitialFramework())
    val currentFramework: StateFlow<CommunicationFramework> = _currentFramework.asStateFlow()

    private fun loadInitialFramework(): CommunicationFramework {
        val saved = settings.getString(KEY_FRAMEWORK, CommunicationFramework.NEW_FRAMEWORK.name)
        return try {
            CommunicationFramework.valueOf(saved)
        } catch (_: Exception) {
            CommunicationFramework.NEW_FRAMEWORK
        }
    }

    fun setFramework(framework: CommunicationFramework) {
        if (_currentFramework.value != framework) {
            KLog.i(TAG, "切换通讯框架: ${_currentFramework.value.displayName} -> ${framework.displayName}")
            _currentFramework.value = framework
            settings.putString(KEY_FRAMEWORK, framework.name)
        }
    }
}
