package com.example.guangnian.feature.face.data.api

import com.example.guangnian.feature.console.data.bean.BaseBean

interface FaceApi {
    /**
     * 通过 TCP 通知机器人抓拍人脸
     */
    suspend fun robotCaptureFace(name: String): Unit

    /**
     * 通过 TCP 删除人脸
     */
    suspend fun deleteFace(name: String): BaseBean
}
