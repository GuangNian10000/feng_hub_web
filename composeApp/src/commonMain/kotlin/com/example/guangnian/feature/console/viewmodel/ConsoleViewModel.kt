package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.MotionControlApi
import com.example.guangnian.feature.console.data.api.NavigationApi
import com.example.guangnian.feature.console.data.api.RouteApi
import com.example.guangnian.feature.console.data.bean.*
import com.example.guangnian.feature.console.data.event.RobotEvent
import com.example.guangnian.feature.console.data.event.RobotEventBus
import com.example.guangnian.core.network.tcp.KmpTcpDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

data class ConsoleUiState(
    val postures: List<PostureBean> = emptyList(),
    val motions: List<MotionBean> = emptyList(),
    val armActions: List<ArmActionBean> = emptyList(),
    val teachActions: List<TeachActionBean> = emptyList(),
    val favoriteActions: List<FavoriteActionBean> = emptyList(),
    val endpoints: List<InspectionPointBean> = emptyList(),
    val routes: List<RouteBean> = emptyList(),
    val maps: List<MapBean> = emptyList(),
    val robotNavStatus: RobotNavStatusBean = RobotNavStatusBean(),
    val isInspectionPaused: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // 交互状态
    val pendingPoint: InspectionPointBean? = null,
    val deleteConfirmPoint: InspectionPointBean? = null
)

class ConsoleViewModel(
    private val motionControlApi: MotionControlApi,
    private val navigationApi: NavigationApi,
    private val routeApi: RouteApi,
    private val eventBus: RobotEventBus
) : ViewModel() {
    private companion object {
        const val TAG = "ConsoleViewModel"
        private val json = Json { ignoreUnknownKeys = true }
    }

    private val _uiState = MutableStateFlow(ConsoleUiState())
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    init {
        // 监听来自 Map 或其他组件的事件
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is RobotEvent.RequestEditPoint -> {
                        _uiState.update { it.copy(pendingPoint = event.point) }
                    }
                    is RobotEvent.RequestDeletePoint -> {
                        _uiState.update { it.copy(deleteConfirmPoint = event.point) }
                    }
                    else -> {}
                }
            }
        }

        // 注册导航状态主动推送监听
        viewModelScope.launch {
            KmpTcpDispatcher.registerHandler("/slam/nav_status/") { requestJson ->
                try {
                    requestJson["msg"]?.let { msgElement ->
                        val navStatus = json.decodeFromJsonElement<RobotNavStatusBean>(msgElement)
                        _uiState.update { it.copy(robotNavStatus = navStatus) }
                        
                        // 同时同步建图状态给 EventBus
                        val isMapping = navStatus.state == RobotNavState.MAPPING
                        eventBus.tryEmit(RobotEvent.MappingStateChanged(isMapping))
                    }
                } catch (e: Exception) {
                    KLog.w(TAG, "解析推送的导航状态失败: ${e.message}")
                }
                null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            KmpTcpDispatcher.unregisterHandler("/slam/nav_status/")
        }
    }

    fun fetchNavStatus() {
        viewModelScope.launch {
            try {
                val status = navigationApi.getNavStatus()
                _uiState.update { it.copy(robotNavStatus = status) }
            } catch (e: Exception) {
                KLog.w(TAG, "获取导航状态失败: ${e.message}")
            }
        }
    }

    fun clearPendingPoint() {
        _uiState.update { it.copy(pendingPoint = null) }
    }

    fun clearDeleteConfirmPoint() {
        _uiState.update { it.copy(deleteConfirmPoint = null) }
    }

    private fun handleError(message: String, throwable: Throwable?) {
        KLog.w(TAG, "$message: ${throwable?.message}")
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
        executeTask("获取导航点失败") {
            val list = navigationApi.getInspectionPoints()
            _uiState.update { it.copy(endpoints = list) }
        }
    }

    fun fetchMaps() {
        executeTask("获取地图列表失败") {
            val response = navigationApi.getMaps()
            _uiState.update { it.copy(maps = response.maps) }
        }
    }

    fun deleteMap(mapUid: String) {
        executeTask("删除地图失败") {
            navigationApi.archiveMap(mapUid)
            fetchMaps()
        }
    }

    fun switchMap(mapUid: String) {
        executeTask("切换地图失败") {
            navigationApi.switchMap(mapUid)
            fetchMaps()
        }
    }

    fun startMapping(mapName: String) {
        executeTask("开始建图失败") {
            navigationApi.startMapping(mapName)
            eventBus.emit(RobotEvent.MappingStateChanged(true))
        }
    }

    fun endMapping() {
        executeTask("结束建图失败") {
            navigationApi.endMapping()
            eventBus.emit(RobotEvent.MappingStateChanged(false))
            fetchMaps() // 结束后刷新列表
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
                KLog.w(TAG, "发送姿态指令失败: ${e.message}")
            }
        }
    }

    fun sendArmCommand(armId: String) {
        viewModelScope.launch {
            try {
                motionControlApi.executeMotion(armId)
            } catch (e: Exception) {
                KLog.w(TAG, "发送机械臂动作失败: ${e.message}")
            }
        }
    }

    fun startInspectionPoint(point_id: String) {
        viewModelScope.launch {
            try {
                navigationApi.startInspectionPoint(point_id)
            } catch (e: Exception) {
                KLog.w(TAG, "开始导航点失败: ${e.message}")
            }
        }
    }

    fun startRoute(route_id: String) {
        viewModelScope.launch {
            try {
                routeApi.startRoute(route_id)
            } catch (e: Exception) {
                KLog.w(TAG, "开始路线失败: ${e.message}")
            }
        }
    }

    fun pauseNavigation() {
        viewModelScope.launch {
            try {
                val res = navigationApi.pauseNavigation()
                if (res.receive) {
                    _uiState.update { it.copy(isInspectionPaused = true) }
                }
            } catch (e: Exception) {
                KLog.w(TAG, "暂停导航失败: ${e.message}")
            }
        }
    }

    fun resumeNavigation() {
        viewModelScope.launch {
            try {
                val res = navigationApi.resumeNavigation()
                if (res.receive) {
                    _uiState.update { it.copy(isInspectionPaused = false) }
                }
            } catch (e: Exception) {
                KLog.w(TAG, "继续导航失败: ${e.message}")
            }
        }
    }

    fun stopNavigation() {
        viewModelScope.launch {
            try {
                navigationApi.stopPosition()
            } catch (e: Exception) {
                KLog.w(TAG, "取消导航失败: ${e.message}")
            }
        }
    }

    fun addInspectionPoint(point: InspectionPointBean) {
        executeTask("添加导航点失败") {
            navigationApi.addInspectionPoint(point)
            fetchInspectionPoints()
            eventBus.emit(RobotEvent.PointsChanged)
        }
    }

    fun updateInspectionPoint(point: InspectionPointBean) {
        executeTask("更新导航点失败") {
            navigationApi.updateInspectionPoint(point)
            fetchInspectionPoints()
            eventBus.emit(RobotEvent.PointsChanged)
        }
    }

    fun deleteInspectionPoint(point_id: String) {
        executeTask("删除导航点失败") {
            navigationApi.deleteInspectionPoint(point_id)
            fetchInspectionPoints()
            eventBus.emit(RobotEvent.PointsChanged)
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

    fun deleteRoute(route_id: String) {
        executeTask("删除路线失败") {
            routeApi.deleteRoute(route_id)
            fetchRoutes()
        }
    }

    fun stopAll() {
        viewModelScope.launch { 
            try {
                motionControlApi.stopAll()
            } catch (e: Exception) {
                KLog.w(TAG, "停止指令发送失败: ${e.message}")
            }
        }
    }

    fun recover() {
        viewModelScope.launch {
            try {
                motionControlApi.recover()
            } catch (e: Exception) {
                KLog.w(TAG, "恢复指令发送失败: ${e.message}")
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
                            KLog.w(TAG, "摇杆指令发送失败: ${e.message}")
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
                    KLog.w(TAG, "停止摇杆指令发送失败: ${e.message}")
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
