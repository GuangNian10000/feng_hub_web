package com.example.guangnian.feature.face.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guangnian.feature.face.data.bean.FaceConfig
import com.example.guangnian.feature.face.data.bean.FaceData
import com.example.guangnian.feature.face.data.repository.FaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceViewModel(
    private val repository: FaceRepository
) : ViewModel() {

    private val _faces = MutableStateFlow<List<FaceData>>(emptyList())
    val faces: StateFlow<List<FaceData>> = _faces.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _config = MutableStateFlow(FaceConfig())
    val config: StateFlow<FaceConfig> = _config.asStateFlow()

    init {
        refresh()
        fetchConfig()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _faces.value = repository.getFaceList()
            _loading.value = false
        }
    }

    private fun fetchConfig() {
        viewModelScope.launch {
            _config.value = repository.getConfig()
        }
    }

    fun updateConfig(config: FaceConfig) {
        viewModelScope.launch {
            _config.value = config
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
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.deleteFace(face.username)
                refresh()
            } catch (e: Exception) {
                _loading.value = false
            }
        }
    }

    fun uploadFaceImage(name: String, imageBytes: ByteArray, description: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.uploadFaceImage(name, description, imageBytes)
                refresh()
            } catch (e: Exception) {
                _loading.value = false
            }
        }
    }

    fun captureFromRobotFrame(name: String, description: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val frame = repository.getCurrentFrame()
                repository.uploadFaceImage(name, description, frame)
                refresh()
            } catch (e: Exception) {
                _loading.value = false
            }
        }
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
