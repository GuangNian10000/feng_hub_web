package com.example.guangnian.feature.face.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.face.data.bean.FaceConfig
import com.example.guangnian.feature.face.data.bean.FaceData
import com.example.guangnian.feature.face.data.repository.FaceRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class FaceViewModel(
    private val repository: FaceRepository
) : ViewModel() {
    private companion object {
        const val TAG = "FaceViewModel"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        KLog.e(TAG, "协程未捕获异常: ${throwable.message}", throwable)
        _loading.value = false
    }

    private val _faces = MutableStateFlow<List<FaceData>>(emptyList())
    val faces: StateFlow<List<FaceData>> = _faces.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _config = MutableStateFlow(FaceConfig())
    val config: StateFlow<FaceConfig> = _config.asStateFlow()

    private val _currentFrame = MutableStateFlow<ByteArray?>(null)
    val currentFrame: StateFlow<ByteArray?> = _currentFrame.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
        fetchConfig()
    }

    private fun safeLaunch(action: String, showLoading: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            if (showLoading) _loading.value = true
            try {
                KLog.d(TAG, "任务开始: $action, showLoading=$showLoading")
                block()
                KLog.d(TAG, "任务成功: $action, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            } catch (e: Exception) {
                KLog.w(TAG, "任务失败: $action, error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
                _errorMessage.value = e.message ?: "Unknown Error"
            } finally {
                if (showLoading) _loading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refresh() {
        safeLaunch(action = "刷新人脸列表", showLoading = true) {
            _faces.value = repository.getFaceList()
        }
    }

    private fun fetchConfig() {
        safeLaunch(action = "获取人脸配置") {
            _config.value = repository.getConfig()
        }
    }

    fun updateConfig(config: FaceConfig) {
        _config.value = config
        safeLaunch(action = "更新人脸配置") {
            repository.updateConfig(config)
        }
    }

    fun updateThreshold(value: Float) {
        updateConfig(_config.value.copy(threshold = value))
    }

    fun toggleCameraFlip(value: Boolean) {
        updateConfig(_config.value.copy(camera_flip = value))
    }

    fun toggleOnlyKnown(value: Boolean) {
        updateConfig(_config.value.copy(only_known = value))
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteFace(face: FaceData) {
        safeLaunch(action = "删除人脸:${face.username}", showLoading = true) {
            repository.deleteFace(face.username)
            _faces.value = repository.getFaceList()
        }
    }

    fun uploadFaceImage(name: String, imageBytes: ByteArray, description: String = "") {
        safeLaunch(action = "上传人脸图片:$name", showLoading = true) {
            repository.uploadFaceImage(name, description, imageBytes)
            _faces.value = repository.getFaceList()
        }
    }

    fun captureFromRobotFrame(name: String, description: String = "") {
        safeLaunch(action = "从机器人画面采集人脸:$name", showLoading = true) {
            val frame = repository.getCurrentFrame()
            repository.uploadFaceImage(name, description, frame)
            _faces.value = repository.getFaceList()
        }
    }

    fun fetchCurrentFrame() {
        safeLaunch(action = "获取当前画面") {
            _currentFrame.value = repository.getCurrentFrame()
        }
    }

    fun clearCurrentFrame() {
        _currentFrame.value = null
    }

    val filteredFaces: List<FaceData>
        get() = if (searchQuery.value.isEmpty()) {
            faces.value
        } else {
            faces.value.filter { 
                it.username.contains(searchQuery.value, ignoreCase = true) || 
                (it.description?.contains(searchQuery.value, ignoreCase = true) == true)
            }
        }
}
