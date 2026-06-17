package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

@Serializable
data class MapConfigBean(
    val image: String = "",
    val resolution: Double = 0.0,
    val origin: List<Double> = emptyList(),
    val negate: Double = 0.0,
    val occupied_thresh: Double = 0.0,
    val free_thresh: Double = 0.0
) {
    /**
     * 将 image 字段中的 IP 替换为指定的 TCP 连接 IP
     * 正则逻辑：匹配 :// 之后直到遇到 : (端口) 或 / (路径) 的部分
     */
    fun getFixedImageUrl(tcpIp: String): String {
        if (image.isEmpty() || tcpIp.isEmpty()) return image

        // 正则解释：(?<=://) 是零宽断言，匹配 :// 之后
        // [^:/]+ 匹配不含 : 或 / 的字符（即 IP 或域名部分）
        val regex = Regex("(?<=://)[^:/]+")
        return image.replace(regex, tcpIp)
    }
}