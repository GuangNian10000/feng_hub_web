package com.example.guangnian.feature.console.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.api.MotionControlApi
import com.example.guangnian.feature.console.data.api.NavigationApi
import com.example.guangnian.feature.console.data.api.RouteApi
import com.example.guangnian.feature.console.data.api.AgentControlApi
import com.example.guangnian.feature.console.data.api.RobotStatusApi
import com.example.guangnian.feature.console.data.bean.*
import com.example.guangnian.feature.console.data.event.RobotEvent
import com.example.guangnian.feature.console.data.event.RobotEventBus
import com.example.guangnian.core.network.tcp.KmpTcpClient
import com.example.guangnian.core.network.tcp.KmpTcpDispatcher
import com.example.guangnian.core.network.tcp.TcpState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlinx.datetime.Clock

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
    val agentStatus: AgentStatusBean? = null,
    val systemInfo: SystemInfo? = null,
    val isInspectionPaused: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
    // 交互状态
    val pendingPoint: InspectionPointBean? = null,
    val deleteConfirmPoint: InspectionPointBean? = null
)

class ConsoleViewModel(
    private val motionControlApi: MotionControlApi,
    private val navigationApi: NavigationApi,
    private val routeApi: RouteApi,
    private val agentControlApi: AgentControlApi,
    private val robotStatusApi: RobotStatusApi,
    private val eventBus: RobotEventBus
) : ViewModel() {
    private companion object {
        const val TAG = "ConsoleViewModel"
        private val json = Json { 
            ignoreUnknownKeys = true 
            coerceInputValues = true
        }
    }

    private val _uiState = MutableStateFlow(ConsoleUiState())
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()
    private var taskSeq = 0L
    private var joystickSeq = 0L
    private var systemInfoSeq = 0L

    init {
        observeConnectionState()
        startSystemInfoPolling()
        // 监听来自 Map 或其他组件的事件
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is RobotEvent.RequestEditPoint -> {
                        KLog.d(TAG, "收到编辑点位事件: pointId=${event.point.point_id}, name=${event.point.name}")
                        _uiState.update { it.copy(pendingPoint = event.point) }
                    }
                    is RobotEvent.RequestDeletePoint -> {
                        KLog.d(TAG, "收到删除点位事件: pointId=${event.point.point_id}, name=${event.point.name}")
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
                        KLog.d(TAG, "收到导航状态推送: state=${navStatus.state}, msg=$msgElement")
                        
                        // 同时同步状态给 EventBus
                        val isMapping = navStatus.state == RobotNavState.MAPPING
                        eventBus.tryEmit(RobotEvent.MappingStateChanged(isMapping))
                        eventBus.tryEmit(RobotEvent.NavStatusChanged(navStatus))
                    }
                } catch (e: Exception) {
                    KLog.w(TAG, "解析推送的导航状态失败: ${e.message}, raw=$requestJson", e)
                }
                null
            }
        }

        viewModelScope.launch {
            KmpTcpDispatcher.registerHandler("/state/get_status/") { requestJson ->
                try {
                    requestJson["msg"]?.let { msgElement ->
                        val status = json.decodeFromJsonElement<AgentStatusBean>(msgElement)
                        _uiState.update { it.copy(agentStatus = status) }
                        KLog.d(
                            TAG,
                            "收到智能体状态推送: isWakeUp=${status.isWakeUp}, isAsrEnabled=${status.isAsrEnabled}"
                        )
                    }
                } catch (e: Exception) {
                    KLog.w(TAG, "解析智能体状态推送失败: ${e.message}, raw=$requestJson", e)
                }
                null
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            KmpTcpClient.connectionState.collect { state ->
                val connected = state == TcpState.CONNECTED
                _uiState.update { it.copy(isConnected = connected) }
                if (connected) {
                    KLog.i(TAG, "TCP连接已建立，开始同步基础数据")
                    refreshAllData()
                }
            }
        }
    }

    /**
     * 刷新所有业务基础数据
     */
    fun refreshAllData() {
        fetchInspectionPoints()
        fetchRoutes()
        fetchMaps()
        fetchNavStatus()
        fetchAgentStatus()
        fetchPostureList()
        fetchArmList()
    }

    private fun startSystemInfoPolling() {
        viewModelScope.launch {
            while (isActive) {
                if (KmpTcpClient.connectionState.value != TcpState.CONNECTED) {
                    delay(2000)
                    continue
                }
                systemInfoSeq++
                try {
                    val info = robotStatusApi.getSystemInfo()
                    _uiState.update { it.copy(systemInfo = info) }
                } catch (e: Exception) {
                    KLog.w(TAG, "获取系统信息失败 #$systemInfoSeq: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            KmpTcpDispatcher.unregisterHandler("/slam/nav_status/")
            KmpTcpDispatcher.unregisterHandler("/state/get_status/")
        }
    }

    fun fetchAgentStatus() {
        viewModelScope.launch {
            try {
                val status = agentControlApi.getAgentStatus()
                _uiState.update { it.copy(agentStatus = status) }
                KLog.i(
                    TAG,
                    "获取智能体状态成功: isWakeUp=${status.isWakeUp}, isAsrEnabled=${status.isAsrEnabled}"
                )
            } catch (e: Exception) {
                KLog.e(TAG, "获取智能体状态失败: ${e.message}")
            }
        }
    }

    fun toggleAgentWakeUp() {
        val status = _uiState.value.agentStatus ?: return
        sendAgentStatus(status.copy(isWakeUp = !status.isWakeUp))
    }

    fun toggleAgentAsr() {
        val status = _uiState.value.agentStatus ?: return
        sendAgentStatus(status.copy(isAsrEnabled = !status.isAsrEnabled))
    }

    private fun sendAgentStatus(status: AgentStatusBean) {
        viewModelScope.launch {
            try {
                agentControlApi.setAgentStatus(status)
            } catch (e: Exception) {
                KLog.e(TAG, "设置智能体状态失败: ${e.message}")
            }
        }
    }

    fun interruptDialogue() {
        viewModelScope.launch {
            try {
                agentControlApi.interruptDialogue()
            } catch (e: Exception) {
                KLog.e(TAG, "打断智能体对话失败: ${e.message}")
            }
        }
    }

    fun setAgentVolume(increase: Boolean) {
        viewModelScope.launch {
            try {
                agentControlApi.setVolume(increase)
            } catch (e: Exception) {
                KLog.e(TAG, "调节智能体音量失败: ${e.message}")
            }
        }
    }

    fun fetchNavStatus() {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "用户操作: 主动获取导航状态")
                val status = navigationApi.getNavStatus()
                _uiState.update { it.copy(robotNavStatus = status) }
                KLog.i(TAG, "获取导航状态成功: state=${status.state}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.e(TAG, "获取导航状态失败: ${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
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
        executeTask("删除地图失败", mapUid) {
            navigationApi.archiveMap(mapUid)
            fetchMaps()
        }
    }

    fun switchMap(mapUid: String) {
        executeTask("切换地图失败", mapUid) {
            navigationApi.switchMap(mapUid)
            // 切换地图后，点位和路线都会改变，需要全量刷新
            refreshAllData()
        }
    }

    fun startMapping(mapName: String) {
        executeTask("开始建图失败", mapName) {
            navigationApi.startMapping(mapName)
            eventBus.emit(RobotEvent.MappingStateChanged(true))
        }
    }

    fun endMapping() {
        executeTask("结束建图失败") {
            navigationApi.endMapping()
            eventBus.emit(RobotEvent.MappingStateChanged(false))
            // 结束后刷新地图列表和可能的初始点位
            refreshAllData()
        }
    }

    fun fetchRoutes() {
        executeTask("获取路线列表失败") {
            val list = routeApi.getRoutes()
            _uiState.update { it.copy(routes = list) }
        }
    }

    private fun executeTask(errorMsg: String, requestData: Any? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            taskSeq += 1
            val seq = taskSeq
            val startTime = Clock.System.now().toEpochMilliseconds()
            //KLog.d(TAG, "任务开始 #$seq: $errorMsg${if (requestData != null) ", request=$requestData" else ""}")
            _uiState.update { it.copy(isLoading = true) }
            try {
                block()
                KLog.d(TAG, "任务成功 #$seq: $errorMsg, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "任务失败 #$seq: $errorMsg, ${if (requestData != null) "request=$requestData, " else ""}error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                handleError(errorMsg, e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- 指令发送 (轻量操作) ---

    fun sendPostureCommand(postureName: String) {
        viewModelScope.launch { 
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始发送姿态指令: postureName=$postureName")
                motionControlApi.executePosture(postureName)
                KLog.d(TAG, "姿态指令发送成功: postureName=$postureName, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "发送姿态指令失败: postureName=$postureName, error=${e.message}", e)
            }
        }
    }

    fun sendArmCommand(armId: String) {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始发送机械臂动作: armId=$armId")
                motionControlApi.executeMotion(armId)
                KLog.d(TAG, "机械臂动作发送成功: armId=$armId, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "发送机械臂动作失败: armId=$armId, error=${e.message}", e)
            }
        }
    }

    fun startInspectionPoint(point_id: String) {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始导航到点位: pointId=$point_id")
                navigationApi.startInspectionPoint(point_id)
                KLog.d(TAG, "导航到点位指令成功: pointId=$point_id, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "开始导航点失败: pointId=$point_id, error=${e.message}", e)
            }
        }
    }

    fun startRoute(route_id: String) {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始路线导航: routeId=$route_id")
                routeApi.startRoute(route_id)
                KLog.d(TAG, "路线导航指令成功: routeId=$route_id, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "开始路线失败: routeId=$route_id, error=${e.message}", e)
            }
        }
    }

    fun pauseNavigation() {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始暂停导航")
                val res = navigationApi.pauseNavigation()
                if (res.receive) {
                    _uiState.update { it.copy(isInspectionPaused = true) }
                }
                KLog.d(TAG, "暂停导航成功: receive=${res.receive}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "暂停导航失败: ${e.message}", e)
            }
        }
    }

    fun resumeNavigation() {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始继续导航")
                val res = navigationApi.resumeNavigation()
                if (res.receive) {
                    _uiState.update { it.copy(isInspectionPaused = false) }
                }
                KLog.d(TAG, "继续导航成功: receive=${res.receive}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "继续导航失败: ${e.message}", e)
            }
        }
    }

    fun stopNavigation() {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始取消导航")
                navigationApi.stopPosition()
                KLog.d(TAG, "取消导航成功: 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "取消导航失败: ${e.message}", e)
            }
        }
    }

    fun addInspectionPoint(point: InspectionPointBean) {
        if (point.point_id.isNotEmpty()) {
            updateInspectionPoint(point)
            return
        }
        executeTask("添加导航点失败", point) {
            navigationApi.addInspectionPoint(point)
            fetchInspectionPoints()
            eventBus.emit(RobotEvent.PointsChanged)
        }
    }

    fun updateInspectionPoint(point: InspectionPointBean) {
        executeTask("更新导航点失败", point) {
            navigationApi.updateInspectionPoint(point)
            fetchInspectionPoints()
            eventBus.emit(RobotEvent.PointsChanged)
        }
    }

    fun deleteInspectionPoint(point_id: String) {
        executeTask("删除导航点失败", point_id) {
            navigationApi.deleteInspectionPoint(point_id)
            fetchInspectionPoints()
            eventBus.emit(RobotEvent.PointsChanged)
        }
    }

    fun addRoute(route: RouteBean) {
        executeTask("添加路线失败", route) {
            routeApi.addRoute(route)
            fetchRoutes()
        }
    }

    fun updateRoute(route: RouteBean) {
        executeTask("更新路线失败", route) {
            routeApi.updateRoute(route)
            fetchRoutes()
        }
    }

    fun deleteRoute(route_id: String) {
        executeTask("删除路线失败", route_id) {
            routeApi.deleteRoute(route_id)
            fetchRoutes()
        }
    }

    fun stopAll() {
        viewModelScope.launch { 
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始发送急停指令")
                motionControlApi.stopAll()
                KLog.d(TAG, "急停指令发送成功: 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "停止指令发送失败: ${e.message}", e)
            }
        }
    }

    fun recover() {
        viewModelScope.launch {
            val startTime = Clock.System.now().toEpochMilliseconds()
            try {
                KLog.i(TAG, "开始发送急停恢复指令")
                motionControlApi.recover()
                KLog.d(TAG, "急停恢复指令发送成功: 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "恢复指令发送失败: ${e.message}", e)
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
        KLog.d(TAG, "摇杆输入: active=$active, leftX=$leftX, leftY=$leftY, rightX=$rightX")

        if (active) {
            if (joystickJob == null || !joystickJob!!.isActive) {
                KLog.i(TAG, "启动摇杆指令循环")
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
                        joystickSeq += 1
                        val seq = joystickSeq
                        val startTime = Clock.System.now().toEpochMilliseconds()

                        try {
                            KLog.d(TAG, "准备发送摇杆指令 #$seq: angle=$angle, translateLevel=$translateLevel, rotateLevel=$rotateLevel")
                            motionControlApi.moveMixed(angle, translateLevel, rotateLevel)
                            KLog.d(TAG, "摇杆指令发送成功 #$seq: angle=$angle, translateLevel=$translateLevel, rotateLevel=$rotateLevel, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                        } catch (e: Exception) {
                            KLog.w(TAG, "摇杆指令发送失败 #$seq: angle=$angle, translateLevel=$translateLevel, rotateLevel=$rotateLevel, error=${e.message}", e)
                        }
                        delay(100)
                    }
                }
            }
        } else {
            KLog.i(TAG, "停止摇杆指令循环: lastSeq=$joystickSeq")
            joystickJob?.cancel()
            joystickJob = null
            viewModelScope.launch { 
                val startTime = Clock.System.now().toEpochMilliseconds()
                try {
                    KLog.d(TAG, "发送摇杆归零指令")
                    motionControlApi.moveMixed(0.0, 0, 0)
                    KLog.d(TAG, "摇杆归零指令发送成功: 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                } catch (e: Exception) {
                    KLog.w(TAG, "停止摇杆指令发送失败: ${e.message}", e)
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
