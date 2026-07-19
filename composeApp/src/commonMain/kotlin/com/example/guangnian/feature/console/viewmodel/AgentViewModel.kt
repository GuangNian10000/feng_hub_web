package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.console.data.api.AgentManagerApi
import com.example.guangnian.feature.console.data.bean.AgentEntity
import com.example.guangnian.feature.console.data.bean.AgentListResponse
import com.example.guangnian.core.utils.KLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentUiState(
    val agentData: AgentListResponse = AgentListResponse(),
    val isLoading: Boolean = false
)

class AgentViewModel(private val api: AgentManagerApi) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private fun executeTask(errorPrefix: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                block()
            } catch (e: Exception) {
                KLog.w("AgentViewModel", "$errorPrefix: ${e.message}", e)
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

    fun selectAgent(agent: AgentEntity) {
        executeTask("切换异常") {
            val res = api.selectAgent(agent)
            _toastEvent.emit(res.msg)
            if (res.st == 200) fetchAgentList()
        }
    }

    fun deleteAgent(agent: AgentEntity) {
        executeTask("删除异常") {
            val res = api.deleteAgent(agent)
            _toastEvent.emit(res.msg)
            if (res.st == 200) fetchAgentList()
        }
    }

    fun editAgent(agent: AgentEntity) {
        executeTask("修改异常") {
            val res = api.editAgent(agent)
            _toastEvent.emit(res.msg)
            if (res.st == 200) fetchAgentList()
        }
    }

    fun addAgent(agent: AgentEntity) {
        executeTask("添加异常") {
            val res = api.addAgent(agent)
            _toastEvent.emit(res.msg)
            if (res.st == 200) fetchAgentList()
        }
    }
}
