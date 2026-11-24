package com.example.guangnian.core.network.model

import kotlinx.serialization.Serializable

@Serializable
open class BaseBean(
    val receive: Boolean = true,
    val st: Int = 0,
    val message: String = "",
    val msg: String = ""
)
