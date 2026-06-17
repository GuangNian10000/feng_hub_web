package com.example.guangnian.feature.console.data.bean

data class FavoriteActionBean(
    val id: String, // 对应 posture_name 或 arm_id 的字符串形式
    val name: String,
    val isArmAction: Boolean // true 为手部动作，false 为姿态动作
)