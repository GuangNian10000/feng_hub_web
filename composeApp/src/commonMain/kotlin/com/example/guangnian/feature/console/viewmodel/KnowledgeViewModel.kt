package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.console.data.api.KnowledgeApi
import com.example.guangnian.feature.console.data.bean.KnowledgeEntity
import com.example.guangnian.feature.console.data.bean.KnowledgeListResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KnowledgeUiState(
    val knowledgeData: KnowledgeListResponse = KnowledgeListResponse(),
    val isLoading: Boolean = false
)

class KnowledgeViewModel(private val api: KnowledgeApi) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState())
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private fun executeTask(errorPrefix: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                block()
            } catch (e: Exception) {
                _toastEvent.emit("$errorPrefix: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchKnowledgeList() {
        executeTask("获取知识库异常") {
            val response = api.getKnowledgeList()
            _uiState.update { it.copy(knowledgeData = response) }
        }
    }

    fun addKnowledge(knowledge: KnowledgeEntity) {
        executeTask("添加异常") {
            val res = api.addKnowledge(knowledge)
            _toastEvent.emit(res.msg)
            if (res.st == 200) fetchKnowledgeList()
        }
    }

    fun editKnowledge(knowledge: KnowledgeEntity) {
        executeTask("修改异常") {
            val res = api.editKnowledge(knowledge)
            _toastEvent.emit(res.msg)
            if (res.st == 200) fetchKnowledgeList()
        }
    }

    fun deleteKnowledge(knowledge: KnowledgeEntity) {
        executeTask("删除异常") {
            val res = api.deleteKnowledge(knowledge)
            _toastEvent.emit(res.msg)
            if (res.st == 200) fetchKnowledgeList()
        }
    }
}
