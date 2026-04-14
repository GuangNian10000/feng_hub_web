package com.example.guangnian.feature.robot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.robot.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.robot.data.bean.ArmActionBean
import com.example.guangnian.feature.robot.data.bean.MotionBean
import com.example.guangnian.feature.robot.data.bean.PostureBean
import com.example.guangnian.feature.robot.data.bean.TeachActionBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 聚合动作模块所有 UI 状态
data class ActionUiState(
    val postures: List<PostureBean> = emptyList(),
    val motions: List<MotionBean> = emptyList(),
    val armActions: List<ArmActionBean> = emptyList(),
    val teachActions: List<TeachActionBean> = emptyList(),
    val isLoading: Boolean = false
)

class ActionViewModel : ViewModel() {

    private companion object {
        const val TAG = "ActionViewModel"
    }

    private val robotApi = RobotTcpApiImpl()

    // 统一状态流
    private val _uiState = MutableStateFlow(ActionUiState())
    val uiState: StateFlow<ActionUiState> = _uiState.asStateFlow()

    // 获取姿态动作列表
    fun fetchPostureList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = robotApi.getPostureList()
                _uiState.update { it.copy(postures = list, isLoading = false) }
            } catch (e: Exception) {
                KLog.e(TAG, "拉取姿态动作列表失败", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 执行姿态动作
    fun sendPostureCommand(postureName: String) {
        viewModelScope.launch {
            try {
                robotApi.executePosture(postureName)
            } catch (e: Exception) {
                KLog.e(TAG, "发送姿态动作指令失败: $postureName", e)
            }
        }
    }

    // 获取运动控制列表
    fun fetchMotionList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = robotApi.getMotionList()
                _uiState.update { it.copy(motions = list, isLoading = false) }
            } catch (e: Exception) {
                KLog.e(TAG, "拉取运动控制列表失败", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 执行运动控制动作
    fun sendMotionCommand(motionId: Int) {
        viewModelScope.launch {
            try {
                robotApi.executeMotion(motionId)
            } catch (e: Exception) {
                KLog.e(TAG, "执行运动控制动作失败: motion_id=$motionId", e)
            }
        }
    }

    // 执行预设运动动作（ahead/back/left/right/turn_left/turn_right/turn_back）
    fun sendSportCommand(sport: String) {
        viewModelScope.launch {
            try {
                robotApi.executeSport(sport)
            } catch (e: Exception) {
                KLog.e(TAG, "执行预设运动动作失败: sport=$sport", e)
            }
        }
    }

    // 获取手臂动作列表
    fun fetchArmList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = robotApi.getArmList()
                _uiState.update { it.copy(armActions = list, isLoading = false) }
            } catch (e: Exception) {
                KLog.e(TAG, "拉取手臂动作列表失败", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 执行手臂动作
    fun sendArmCommand(armId: Int) {
        viewModelScope.launch {
            try {
                robotApi.executeArm(armId)
            } catch (e: Exception) {
                KLog.e(TAG, "执行手臂动作失败: arm_id=$armId", e)
            }
        }
    }

    // 平移摇杆控制
    fun moveTranslate(angle: Double, level: Int) {
        viewModelScope.launch {
            try {
                robotApi.moveTranslate(angle, level)
            } catch (e: Exception) {
                KLog.e(TAG, "平移摇杆控制失败: angle=$angle, level=$level", e)
            }
        }
    }

    // 旋转摇杆控制
    fun moveRotate(level: Int) {
        viewModelScope.launch {
            try {
                robotApi.moveRotate(level)
            } catch (e: Exception) {
                KLog.e(TAG, "旋转摇杆控制失败: level=$level", e)
            }
        }
    }

    // 停止平移摇杆
    fun stopTranslate() {
        viewModelScope.launch {
            try {
                robotApi.stopTranslate()
            } catch (e: Exception) {
                KLog.e(TAG, "停止平移摇杆失败", e)
            }
        }
    }

    // 停止旋转摇杆
    fun stopRotate() {
        viewModelScope.launch {
            try {
                robotApi.stopRotate()
            } catch (e: Exception) {
                KLog.e(TAG, "停止旋转摇杆失败", e)
            }
        }
    }

    // 开始录制示教动作
    fun startTeach(name: String) {
        viewModelScope.launch {
            try {
                robotApi.startTeach(name)
            } catch (e: Exception) {
                KLog.e(TAG, "开始录制示教动作失败: name=$name", e)
            }
        }
    }

    // 结束录制示教动作
    fun stopTeach() {
        viewModelScope.launch {
            try {
                robotApi.stopTeach()
            } catch (e: Exception) {
                KLog.e(TAG, "结束录制示教动作失败", e)
            }
        }
    }

    // 删除示教动作
    fun deleteTeach(name: String) {
        viewModelScope.launch {
            try {
                robotApi.deleteTeach(name)
            } catch (e: Exception) {
                KLog.e(TAG, "删除示教动作失败: name=$name", e)
            }
        }
    }

    // 重命名示教动作
    fun renameTeach(oldName: String, newName: String) {
        viewModelScope.launch {
            try {
                robotApi.renameTeach(oldName, newName)
            } catch (e: Exception) {
                KLog.e(TAG, "重命名示教动作失败: old=$oldName, new=$newName", e)
            }
        }
    }

    // 获取示教动作列表
    fun fetchTeachActionList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = robotApi.getTeachActionList()
                _uiState.update { it.copy(teachActions = list, isLoading = false) }
            } catch (e: Exception) {
                KLog.e(TAG, "拉取示教动作列表失败", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 执行示教动作
    fun executeTeach(name: String) {
        viewModelScope.launch {
            try {
                robotApi.executeTeach(name)
            } catch (e: Exception) {
                KLog.e(TAG, "执行示教动作失败: name=$name", e)
            }
        }
    }

    // 急停（停止运动/建图）
    fun stopAll() {
        viewModelScope.launch {
            try {
                robotApi.stopAll()
            } catch (e: Exception) {
                KLog.e(TAG, "急停失败", e)
            }
        }
    }
}