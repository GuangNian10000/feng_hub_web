package com.example.guangnian.feature.motion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.motion.data.api.ActionEditingApi
import com.example.guangnian.feature.console.data.bean.ArmActionBean
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
import kotlinx.datetime.Clock

data class ActionEditingUiState(
    val actionList: List<ArmActionBean> = emptyList(),
    val teachStatus: TeachStatusBean = TeachStatusBean(),
    val isLoading: Boolean = false
)

class ActionEditingViewModel(private val api: ActionEditingApi) : ViewModel() {
    private companion object {
        const val TAG = "ActionEditingViewModel"
    }

    private val _uiState = MutableStateFlow(ActionEditingUiState())
    val uiState: StateFlow<ActionEditingUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        fetchActionEditingStatus()
        fetchActionEditingList()
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

    fun fetchActionEditingStatus() {
        executeTask("查询动作编辑状态异常", showLoading = false) {
            val status = api.getActionEditingRecordStatus()
            _uiState.update { it.copy(teachStatus = status) }
        }
    }

    fun fetchActionEditingList() {
        executeTask("获取动作编辑列表异常") {
            val list = api.getActionEditingList()
            _uiState.update { it.copy(actionList = list) }
        }
    }

    fun startActionEditing() {
        executeTask("开始录制异常") {
            api.setActionEditingSession("unlock")
            delay(1000)
            api.recordActionEditing("start")
            // 重新查询状态
            fetchActionEditingStatus()
        }
    }

    fun stopActionEditing(name: String) {
        executeTask("结束录制异常") {
            api.recordActionEditing("stop_save", name)
            delay(1000)
            api.setActionEditingSession("restore")
            // 等待 2 秒后重新请求列表
            delay(2000)
            fetchActionEditingStatus()
            fetchActionEditingList()
        }
    }

    fun deleteActionEditing(name: String) {
        executeTask("删除动作异常") {
            api.deleteActionEditing(name)
            fetchActionEditingList()
        }
    }

    fun renameActionEditing(oldName: String, newName: String) {
        executeTask("重命名动作异常") {
            api.renameActionEditing(oldName, newName)
            fetchActionEditingList()
        }
    }

    fun executeActionEditing(armId: String, name: String) {
        executeTask("执行动作异常") {
            api.executeActionEditing(armId)
            _toastEvent.emit("开始执行动作: $name")
        }
    }

    fun setSessionState(phase: String) {
        executeTask("设置会话异常") {
            api.setActionEditingSession(phase)
            _toastEvent.emit("已" + if (phase == "unlock") "解锁" else "恢复")
        }
    }
}
