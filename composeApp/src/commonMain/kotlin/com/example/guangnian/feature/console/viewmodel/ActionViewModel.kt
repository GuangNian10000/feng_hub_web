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

    private fun handleError(message: String, e: Exception) {
        KLog.e(TAG, "$message: ${e.message}")
        viewModelScope.launch {
            _errorEvents.emit("$message: 请检查网络连接")
        }
    }

    fun fetchPostureList() {
        executeWithLoading {
            val list = motionControlApi.getPostureList()
            _uiState.update { it.copy(postures = list) }
        }
    }

    private fun executeWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                block()
            } catch (e: Exception) {
                handleError("操作失败", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sendPostureCommand(postureName: String) {
        viewModelScope.launch { 
            try {
                motionControlApi.executePosture(postureName) 
            } catch (e: Exception) {
                handleError("发送姿态指令失败", e)
            }
        }
    }

    fun fetchArmList() {
        executeWithLoading {
            val list = motionControlApi.getArmList()
            _uiState.update { it.copy(armActions = list) }
        }
    }

    fun sendArmCommand(armId: Int) {
        viewModelScope.launch { 
            try {
                motionControlApi.executeArm(armId)
            } catch (e: Exception) {
                handleError("发送机械臂指令失败", e)
            }
        }
    }

    // --- 巡检/导览点位业务逻辑 ---

    fun fetchInspectionPoints() {
        executeWithLoading {
            val list = navigationApi.getInspectionPoints()
            _uiState.update { it.copy(endpoints = list) }
        }
    }

    fun addInspectionPoint(point: InspectionPointBean) {
        viewModelScope.launch {
            try {
                val result = navigationApi.addInspectionPoint(point)
                if (result.st == 0) {
                    fetchInspectionPoints()
                } else {
                    _errorEvents.emit("添加失败: ${result.msg}")
                }
            } catch (e: Exception) {
                handleError("添加巡检点失败", e)
            }
        }
    }

    fun deleteInspectionPoint(pointId: String) {
        viewModelScope.launch {
            try {
                val result = navigationApi.deleteInspectionPoint(pointId)
                if (result.st == 0) {
                    fetchInspectionPoints() // 删除成功后刷新列表
                }
            } catch (e: Exception) {
                KLog.e(TAG, "删除巡检点失败", e)
            }
        }
    }

    fun updateInspectionPoint(point: InspectionPointBean) {
        viewModelScope.launch {
            try {
                val result = navigationApi.updateInspectionPoint(point)
                if (result.st == 0) {
                    fetchInspectionPoints() // 更新成功后刷新列表
                }
            } catch (e: Exception) {
                KLog.e(TAG, "更新巡检点失败", e)
            }
        }
    }

    fun startInspectionPoint(pointId: String) {
        viewModelScope.launch {
            try {
                navigationApi.startInspectionPoint(pointId)
            } catch (e: Exception) {
                KLog.e(TAG, "前往巡检点失败", e)
            }
        }
    }

    // --- 路线管理业务逻辑 ---

    fun fetchRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                KLog.d(TAG, "开始请求路线列表...")
                val list = routeApi.getRoutes()
                _uiState.update { it.copy(routes = list, isLoading = false) }
                KLog.d(TAG, "成功获取路线: ${list.size}个")
            } catch (e: Exception) {
                KLog.e(TAG, "获取路线列表失败", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addRoute(route: RouteBean) {
        viewModelScope.launch {
            try {
                val result = routeApi.addRoute(route)
                if (result.st == 0) {
                    fetchRoutes()
                }
            } catch (e: Exception) {
                KLog.e(TAG, "添加路线失败", e)
            }
        }
    }

    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            try {
                val result = routeApi.deleteRoute(routeId)
                if (result.st == 0) {
                    fetchRoutes()
                }
            } catch (e: Exception) {
                KLog.e(TAG, "删除路线失败", e)
            }
        }
    }

    fun updateRoute(route: RouteBean) {
        viewModelScope.launch {
            try {
                val result = routeApi.updateRoute(route)
                if (result.st == 0) {
                    fetchRoutes()
                }
            } catch (e: Exception) {
                KLog.e(TAG, "更新路线失败", e)
            }
        }
    }

    fun startRoute(routeId: String) {
        viewModelScope.launch {
            try {
                routeApi.startRoute(routeId)
            } catch (e: Exception) {
                KLog.e(TAG, "开始路线巡检失败", e)
            }
        }
    }

    fun stopAll() {
        viewModelScope.launch { motionControlApi.stopAll() }
    }

    fun moveMixed(angle: Double, translateLevel: Int, rotateLevel: Int) {
        viewModelScope.launch { motionControlApi.moveMixed(angle, translateLevel, rotateLevel) }
    }

    fun stopTranslate() { viewModelScope.launch { motionControlApi.stopTranslate() } }
    fun stopRotate() { viewModelScope.launch { motionControlApi.stopRotate() } }

    fun toggleFavorite(id: String, name: String, isArmAction: Boolean) {
        _uiState.update { currentState ->
            val currentFavorites = currentState.favoriteActions.toMutableList()
            val existingIndex = currentFavorites.indexOfFirst { it.id == id && it.isArmAction == isArmAction }
            if (existingIndex != -1) currentFavorites.removeAt(existingIndex)
            else currentFavorites.add(FavoriteActionBean(id, name, isArmAction))
            currentState.copy(favoriteActions = currentFavorites)
        }
    }

    // --- 摇杆控制逻辑 ---

    private var joystickJob: Job? = null
    private var lastLeftX = 0f
    private var lastLeftY = 0f
    private var lastRightX = 0f

    /**
     * 更新摇杆输入并自动管理发送循环
     */
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
                            KLog.e(TAG, "发送摇杆指令失败", e)
                        }
                        delay(100)
                    }
                }
            }
        } else {
            joystickJob?.cancel()
            joystickJob = null
            stopTranslate()
            stopRotate()
        }
    }
}
