package com.example.guangnian.feature.robot.data.bean

import kotlinx.serialization.Serializable

/**
 * @author 光年
 * @since 2026/3/2
 * @summary
 */
@Serializable
data class VoiceBean(
    var id: String = "zh_male_shaonianzixin_moon_bigtts",
    var language: String = "中/英", //language  latitude
    var sex: String = "少年梓辛/Brayan", // gender name
    val source: SourceType = SourceType.VOLCANO,    // 标识来源："microsoft" 或 "volcano"
    val locale: String = ""
    )

@Serializable
enum class SourceType{
    MICROSOFT,
    VOLCANO
}