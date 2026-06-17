package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.console.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.console.data.bean.KnowledgeEntity
import com.example.guangnian.feature.console.data.bean.KnowledgeListResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KnowledgeViewModel : ViewModel() {
    private val api = RobotTcpApiImpl()

    private val _knowledgeData = MutableStateFlow(KnowledgeListResponse())
    val knowledgeData: StateFlow<KnowledgeListResponse> = _knowledgeData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun fetchKnowledgeList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getKnowledgeList()
                _knowledgeData.value = response
            } catch (e: Exception) {
                _toastEvent.emit("获取知识库异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addKnowledge(knowledge: KnowledgeEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.addKnowledge(knowledge)
                _toastEvent.emit(res.msg)
                if (res.st == 200) fetchKnowledgeList()
            } catch (e: Exception) {
                _toastEvent.emit("添加异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun editKnowledge(knowledge: KnowledgeEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.editKnowledge(knowledge)
                _toastEvent.emit(res.msg)
                if (res.st == 200) fetchKnowledgeList()
            } catch (e: Exception) {
                _toastEvent.emit("修改异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteKnowledge(knowledge: KnowledgeEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = api.deleteKnowledge(knowledge)
                _toastEvent.emit(res.msg)
                if (res.st == 200) fetchKnowledgeList()
            } catch (e: Exception) {
                _toastEvent.emit("删除异常: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
