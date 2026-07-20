package com.example.guangnian.feature.teach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.teach.data.api.TeachApi
import com.example.guangnian.feature.console.data.bean.TeachActionBean
import com.example.guangnian.feature.console.data.bean.TeachStatusBean
import com.example.guangnian.core.utils.KLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class TeachUiState(
    val actionList: List<TeachActionBean> = emptyList(),
    val teachStatus: TeachStatusBean = TeachStatusBean(),
    val isLoading: Boolean = false
)

class TeachViewModel(private val api: TeachApi) : ViewModel() {
    private companion object {
        const val TAG = "TeachViewModel"
    }

    private val _uiState = MutableStateFlow(TeachUiState())
    val uiState: StateFlow<TeachUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        fetchTeachStatus()
        fetchTeachList()
    }

    private fun executeTask(errorPrefix: String, showLoading: Boolean = true, block: suspend () -> Unit) {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            KLog.d(TAG, "任务开始: $errorPrefix")
            if (showLoading) _uiState.update { it.copy(isLoading = true) }
            try {
                block()
                KLog.d(TAG, "任务成功: $errorPrefix, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "任务失败: $errorPrefix, error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                _toastEvent.emit("$errorPrefix: ${e.message}")
            } finally {
                if (showLoading) _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchTeachStatus() {
        executeTask("查询示教状态异常", showLoading = false) {
            val status = api.getTeachStatus()
            _uiState.update { it.copy(teachStatus = status) }
        }
    }

    fun fetchTeachList() {
        executeTask("获取示教动作列表异常") {
            val list = api.getTeachActionList()
            _uiState.update { it.copy(actionList = list) }
        }
    }

    fun startTeach(name: String) {
        executeTask("开始录制异常") {
            api.startTeach(name)
            // 重新查询状态
            fetchTeachStatus()
        }
    }

    fun stopTeach() {
        executeTask("结束录制异常") {
            api.stopTeach()
            // 等待 2 秒后重新请求列表
            delay(2000)
            fetchTeachStatus()
            fetchTeachList()
        }
    }

    fun deleteTeach(name: String) {
        executeTask("删除示教动作异常") {
            api.deleteTeach(name)
            fetchTeachList()
        }
    }

    fun renameTeach(oldName: String, newName: String) {
        executeTask("重命名示教动作异常") {
            api.renameTeach(oldName, newName)
            fetchTeachList()
        }
    }

    fun executeTeach(name: String) {
        executeTask("执行示教动作异常") {
            api.executeTeach(name)
            _toastEvent.emit("开始执行动作: $name")
        }
    }
}
