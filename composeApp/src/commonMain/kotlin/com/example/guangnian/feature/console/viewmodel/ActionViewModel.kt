package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.console.data.bean.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActionUiState(
    val postures: List<PostureBean> = emptyList(),
    val motions: List<MotionBean> = emptyList(),
    val armActions: List<ArmActionBean> = emptyList(),
    val teachActions: List<TeachActionBean> = emptyList(),
    val favoriteActions: List<FavoriteActionBean> = emptyList(),
    val endpoints: List<InspectionPointBean> = emptyList(),
    val routes: List<RouteBean> = emptyList(),
    val isLoading: Boolean = false
)

class ActionViewModel : ViewModel() {

    private companion object {
        const val TAG = "ActionViewModel"
    }

    private val robotApi = RobotTcpApiImpl()
    private val _uiState = MutableStateFlow(ActionUiState())
    val uiState: StateFlow<ActionUiState> = _uiState.asStateFlow()

    fun fetchPostureList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = robotApi.getPostureList()
                _uiState.update { it.copy(postures = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sendPostureCommand(postureName: String) {
        viewModelScope.launch { robotApi.executePosture(postureName) }
    }

    fun fetchArmList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = robotApi.getArmList()
                _uiState.update { it.copy(armActions = list, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sendArmCommand(armId: Int) {
        viewModelScope.launch { robotApi.executeArm(armId) }
    }

    // --- 巡检/导览点位业务逻辑 ---

    fun fetchInspectionPoints() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                KLog.d(TAG, "开始请求巡检点列表...")
                val list = robotApi.getInspectionPoints()
                _uiState.update { it.copy(endpoints = list, isLoading = false) }
                KLog.d(TAG, "成功获取巡检点: ${list.size}个")
            } catch (e: Exception) {
                KLog.e(TAG, "获取巡检点列表失败", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addInspectionPoint(point: InspectionPointBean) {
        viewModelScope.launch {
            try {
                val result = robotApi.addInspectionPoint(point)
                if (result.st == 0) {
                    fetchInspectionPoints() // 添加成功后刷新列表
                }
            } catch (e: Exception) {
                KLog.e(TAG, "添加巡检点失败", e)
            }
        }
    }

    fun deleteInspectionPoint(pointId: String) {
        viewModelScope.launch {
            try {
                val result = robotApi.deleteInspectionPoint(pointId)
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
                val result = robotApi.updateInspectionPoint(point)
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
                robotApi.startInspectionPoint(pointId)
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
                val list = robotApi.getRoutes()
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
                val result = robotApi.addRoute(route)
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
                val result = robotApi.deleteRoute(routeId)
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
                val result = robotApi.updateRoute(route)
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
                robotApi.startRoute(routeId)
            } catch (e: Exception) {
                KLog.e(TAG, "开始路线巡检失败", e)
            }
        }
    }

    fun stopAll() {
        viewModelScope.launch { robotApi.stopAll() }
    }

    fun moveMixed(angle: Double, translateLevel: Int, rotateLevel: Int) {
        viewModelScope.launch { robotApi.moveMixed(angle, translateLevel, rotateLevel) }
    }

    fun stopTranslate() { viewModelScope.launch { robotApi.stopTranslate() } }
    fun stopRotate() { viewModelScope.launch { robotApi.stopRotate() } }

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
