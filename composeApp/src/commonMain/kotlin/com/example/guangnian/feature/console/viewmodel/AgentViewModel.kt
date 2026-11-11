package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.AgentManagerApi
import com.example.guangnian.feature.console.data.bean.AgentListResponse
import com.example.guangnian.feature.console.data.bean.VoiceAgent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AgentUiState(
    val agentData: AgentListResponse = AgentListResponse(),
    val isLoading: Boolean = false
)

class AgentViewModel(private val api: AgentManagerApi) : ViewModel() {
    private companion object {
        const val TAG = "AgentViewModel"
    }

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private fun executeTask(errorPrefix: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            KLog.d(TAG, "任务开始: $errorPrefix")
            _uiState.update { it.copy(isLoading = true) }
            try {
                block()
                KLog.d(TAG, "任务成功: $errorPrefix, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "任务失败: $errorPrefix, error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                _toastEvent.emit("$errorPrefix: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchAgentList() {
        executeTask("获取列表异常") {
            val response = api.getAgentList()
            _uiState.update { it.copy(agentData = response) }
        }
    }

    fun selectAgent(agent: VoiceAgent) {
        executeTask("切换异常") {
            val res = api.selectAgent(agent)
            val tip = res.msg.ifEmpty { res.message }.ifEmpty { "已应用智能体: ${agent.voiceName}" }
            _toastEvent.emit(tip)
            fetchAgentList()
        }
    }

    fun deleteAgent(agent: VoiceAgent) {
        executeTask("删除异常") {
            val res = api.deleteAgent(agent)
            val tip = res.msg.ifEmpty { res.message }.ifEmpty { "已删除智能体" }
            _toastEvent.emit(tip)
            fetchAgentList()
        }
    }

    fun editAgent(agent: VoiceAgent) {
        executeTask("修改异常") {
            val res = api.editAgent(agent)
            val tip = res.msg.ifEmpty { res.message }.ifEmpty { "已保存智能体配置" }
            _toastEvent.emit(tip)
            fetchAgentList()
        }
    }

    fun addAgent(agent: VoiceAgent) {
        executeTask("添加异常") {
            val res = api.addAgent(agent)
            val tip = res.msg.ifEmpty { res.message }.ifEmpty { "已添加智能体" }
            _toastEvent.emit(tip)
            fetchAgentList()
        }
    }
}
