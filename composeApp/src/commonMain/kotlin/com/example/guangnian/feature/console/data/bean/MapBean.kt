package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class MapBean(
    val map_id: Int,
    val map_uid: String,
    val map_name: String,
    val status: String,
    val is_active: Boolean,
    val yaml_path: String? = null,
    val pgm_filename: String? = null,
    val pcd_path: String? = null,
    val ground_pcd_path: String? = null,
    val key_poses_path: String? = null,
    val runtime_fingerprint: String? = null,
    val runtime_supported: Boolean = false,
    val last_error: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val image_url:String = ""
)

@Serializable
data class MapListResponse(
    val receive: String = "",
    val active_map_uid: String = "",
    val maps: List<MapBean> = emptyList()
)
