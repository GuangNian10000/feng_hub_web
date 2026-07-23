package com.example.guangnian.feature.face.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class FaceData(
    val id: Int = 0,
    val faceId: String? = null,
    val username: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class FaceListResponse(
    val faces: List<FaceRemoteData>? = null
)

@Serializable
data class FaceRemoteData(
    val name: String = "",
    val desc: String? = null,
    val image_url: String? = null
)

@Serializable
data class FaceConfig(
    val threshold: Float = 0.8f,
    val camera_flip: Boolean = false,
    val only_known: Boolean = false
)

@Serializable
data class CommonResponse(
    val status: String = "",
    val message: String = ""
)
