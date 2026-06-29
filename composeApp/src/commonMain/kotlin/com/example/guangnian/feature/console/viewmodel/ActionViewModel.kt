package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.MotionControlApi
import com.example.guangnian.feature.console.data.api.NavigationApi
import com.example.guangnian.feature.console.data.api.RouteApi
import com.example.guangnian.feature.console.data.bean.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

data class ActionUiState(
    val postures: List<PostureBean> = emptyList(),
    val motions: List<MotionBean> = emptyList(),
    val armActions: List<ArmActionBean> = emptyList(),
    val teachActions: List<TeachActionBean> = emptyList(),
    val favoriteActions: List<FavoriteActionBean> = emptyList(),
    val endpoints: List<InspectionPointBean> = emptyList(),
    val routes: List<RouteBean> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ActionViewModel(
    private val motionControlApi: MotionControlApi,
    private val navigationApi: NavigationApi,
    private val routeApi: RouteApi
) : ViewModel() {
    private companion object {
        const val TAG = "ActionViewModel"
    }

    private val _uiState = MutableStateFlow(ActionUiState())
    val uiState: StateFlow<ActionUiState> = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    private fun handleError(message: String, throwable: Throwable?) {
        KLog.e(TAG, "$message: ${throwable?.message}")
        viewModelScope.launch {
            _errorEvents.emit("$message: 请检查网络连接")
        }
    }

    fun fetchPostureList() {
        executeTask("获取姿态列表失败") {
            val list = motionControlApi.getPostureList()
            _uiState.update { it.copy(postures = list) }
        }
    }

    fun fetchArmList() {
        executeTask("获取机械臂动作失败") {
            val list = motionControlApi.getArmList()
            _uiState.update { it.copy(armActions = list) }
        }
    }

    fun fetchInspectionPoints() {
        executeTask("获取巡检点失败") {
            val list = navigationApi.getInspectionPoints()
            _uiState.update { it.copy(endpoints = list) }
        }
    }

    fun fetchRoutes() {
        executeTask("获取路线列表失败") {
            val list = routeApi.getRoutes()
            _uiState.update { it.copy(routes = list) }
        }
    }

    private fun executeTask(errorMsg: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                block()
            } catch (e: Exception) {
                handleError(errorMsg, e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- 指令发送 (轻量操作) ---

    fun sendPostureCommand(postureName: String) {
        viewModelScope.launch { 
            try {
                motionControlApi.executePosture(postureName)
            } catch (e: Exception) {
                KLog.e(TAG, "发送姿态指令失败: ${e.message}")
            }
        }
    }

    fun sendArmCommand(armId: Int) {
        viewModelScope.launch {
            try {
                motionControlApi.executeMotion(armId)
            } catch (e: Exception) {
                KLog.e(TAG, "发送机械臂动作失败: ${e.message}")
            }
        }
    }

    fun startInspectionPoint(pointId: String) {
        viewModelScope.launch {
            try {
                navigationApi.startInspectionPoint(pointId)
            } catch (e: Exception) {
                KLog.e(TAG, "开始巡检点失败: ${e.message}")
            }
        }
    }

    fun startRoute(routeId: String) {
        viewModelScope.launch {
            try {
                routeApi.startRoute(routeId)
            } catch (e: Exception) {
                KLog.e(TAG, "开始路线失败: ${e.message}")
            }
        }
    }

    fun addInspectionPoint(point: InspectionPointBean) {
        executeTask("添加巡检点失败") {
            navigationApi.addInspectionPoint(point)
            fetchInspectionPoints()
        }
    }

    fun updateInspectionPoint(point: InspectionPointBean) {
        executeTask("更新巡检点失败") {
            navigationApi.updateInspectionPoint(point)
            fetchInspectionPoints()
        }
    }

    fun deleteInspectionPoint(pointId: String) {
        executeTask("删除巡检点失败") {
            navigationApi.deleteInspectionPoint(pointId)
            fetchInspectionPoints()
        }
    }

    fun addRoute(route: RouteBean) {
        executeTask("添加路线失败") {
            routeApi.addRoute(route)
            fetchRoutes()
        }
    }

    fun updateRoute(route: RouteBean) {
        executeTask("更新路线失败") {
            routeApi.updateRoute(route)
            fetchRoutes()
        }
    }

    fun deleteRoute(routeId: String) {
        executeTask("删除路线失败") {
            routeApi.deleteRoute(routeId)
            fetchRoutes()
        }
    }

    fun stopAll() {
        viewModelScope.launch { 
            try {
                motionControlApi.stopAll()
            } catch (e: Exception) {
                KLog.e(TAG, "停止指令发送失败: ${e.message}")
            }
        }
    }

    // --- 摇杆控制 ---

    private var joystickJob: Job? = null
    private var lastLeftX = 0f
    private var lastLeftY = 0f
    private var lastRightX = 0f

    fun updateJoystickInput(leftX: Float, leftY: Float, rightX: Float, active: Boolean) {
        lastLeftX = leftX
        lastLeftY = leftY
        lastRightX = rightX

        if (active) {
            if (joystickJob == null || !joystickJob!!.isActive) {
                joystickJob = viewModelScope.launch {
                    while (isActive) {
                        val distance = hypot(lastLeftX, lastLeftY)
                        val translateLevel = (distance * 10).toInt().coerceIn(0, 10)
                        var angle = 0.0
                        if (translateLevel > 0) {
                            angle = atan2(-lastLeftX.toDouble(), lastLeftY.toDouble()) * 180 / PI
                            if (angle < 0) angle += 360
                        }
                        val rotateLevel = (-lastRightX * 10).toInt().coerceIn(-10, 10)

                        try {
                            motionControlApi.moveMixed(angle, translateLevel, rotateLevel)
                        } catch (e: Exception) {
                            KLog.e(TAG, "摇杆指令发送失败: ${e.message}")
                        }
                        delay(100)
                    }
                }
            }
        } else {
            joystickJob?.cancel()
            joystickJob = null
            viewModelScope.launch { 
                try {
                    motionControlApi.moveMixed(0.0, 0, 0)
                } catch (e: Exception) {
                    KLog.e(TAG, "停止摇杆指令发送失败: ${e.message}")
                }
            }
        }
    }

    fun toggleFavorite(id: String, name: String, isArmAction: Boolean) {
        _uiState.update { currentState ->
            val currentFavorites = currentState.favoriteActions.toMutableList()
            val existingIndex = currentFavorites.indexOfFirst { it.id == id && it.isArmAction == isArmAction }
            if (existingIndex != -1) currentFavorites.removeAt(existingIndex)
            else currentFavorites.add(FavoriteActionBean(id, name, isArmAction))
            currentState.copy(favoriteActions = currentFavorites)
        }
    }
}
