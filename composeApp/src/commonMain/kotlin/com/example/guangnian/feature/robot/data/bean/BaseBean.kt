package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class BaseBean (
    val st :Int = 0,
    val msg :String = ""
)