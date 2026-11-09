package com.example.guangnian.core.network.ws

import com.example.guangnian.core.utils.KLog
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 手机端全局通讯框架与模式管理器
 * 负责持久化存储与管理当前所选框架（新框架 TCP / 老框架 WS）以及老框架模式（模式 A / 模式 B）
 */
class FrameworkManager(private val settings: Settings) {
    companion object {
        private const val TAG = "FrameworkManager"
        const val KEY_FRAMEWORK = "communication_framework"
        const val KEY_SERVER_MODE = "server_mode"

        var instance: FrameworkManager? = null
            private set
    }

    init {
        instance = this
    }

    private val _currentFramework = MutableStateFlow(loadInitialFramework())
    val currentFramework: StateFlow<CommunicationFramework> = _currentFramework.asStateFlow()

    private val _serverMode = MutableStateFlow(loadInitialServerMode())
    val serverMode: StateFlow<ServerMode> = _serverMode.asStateFlow()

    private fun loadInitialFramework(): CommunicationFramework {
        val saved = settings.getString(KEY_FRAMEWORK, CommunicationFramework.NEW_FRAMEWORK.name)
        return try {
            CommunicationFramework.valueOf(saved)
        } catch (_: Exception) {
            CommunicationFramework.NEW_FRAMEWORK
        }
    }

    private fun loadInitialServerMode(): ServerMode {
        val saved = settings.getString(KEY_SERVER_MODE, ServerMode.MODE_A.name)
        return try {
            ServerMode.valueOf(saved)
        } catch (_: Exception) {
            ServerMode.MODE_A
        }
    }

    fun setFramework(framework: CommunicationFramework) {
        if (_currentFramework.value != framework) {
            KLog.i(TAG, "切换通讯框架: ${_currentFramework.value.displayName} -> ${framework.displayName}")
            _currentFramework.value = framework
            settings.putString(KEY_FRAMEWORK, framework.name)
        }
    }

    fun setServerMode(mode: ServerMode) {
        if (_serverMode.value != mode) {
            KLog.i(TAG, "切换老框架通讯模式: ${_serverMode.value.displayName} -> ${mode.displayName}")
            _serverMode.value = mode
            settings.putString(KEY_SERVER_MODE, mode.name)
        }
    }
}
