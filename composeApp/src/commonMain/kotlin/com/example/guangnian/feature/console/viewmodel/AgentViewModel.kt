// AgentViewModel.kt 完全替换
package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.console.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.console.data.bean.AgentEntity
import com.example.guangnian.feature.console.data.bean.AgentListResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {
    private val api = RobotTcpApiImpl()

    private val _agentData = MutableStateFlow(AgentListResponse())
    val agentData: StateFlow<AgentListResponse> = _agentData.asStateFlow()

    // 全局加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Toast 事件流
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun fetchAgentList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getAgentList()
                _agentData.value = response
            } catch (e: Exception) {
                _toastEvent.emit("获取列表异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAgent(agent: AgentEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.selectAgent(agent)
                _toastEvent.emit(res.msg)
                if (res.st == 200) fetchAgentList()
            } catch (e: Exception) {
                _toastEvent.emit("切换异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAgent(agent: AgentEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.deleteAgent(agent)
                _toastEvent.emit(res.msg)
                if (res.st == 200) fetchAgentList()
            } catch (e: Exception) {
                _toastEvent.emit("删除异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun editAgent(agent: AgentEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.editAgent(agent)
                _toastEvent.emit(res.msg)
                if (res.st == 200) fetchAgentList()
            } catch (e: Exception) {
                _toastEvent.emit("修改异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addAgent(agent: AgentEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.addAgent(agent)
                _toastEvent.emit(res.msg)
                if (res.st == 200) fetchAgentList()
            } catch (e: Exception) {
                _toastEvent.emit("添加异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}