package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.Serializable

enum class VoiceEngineType(val id: String, val label: String) {
    BASIC("basic", "基础版 (国内超拟人)"),
    INTERNATIONAL("international", "国际版 (多语种/大模型)")
}

@Serializable
data class VoiceItem(
    val id: String,
    val vcn: String = "",
    val name: String,
    val language: String = "zh",
    val languages: List<String> = emptyList(),
    val language_label: String = "",
    val scene: String = "",
    val tone: String = "",
    val pro: Boolean = false,
    val regions: List<String> = emptyList(),
    val latitude: String = ""
) {
    val effectiveVcn: String
        get() = if (vcn.isNotEmpty()) vcn else id

    val isDialect: Boolean
        get() = latitude.contains("方言") || name.contains("川") || name.contains("粤") ||
                name.contains("东北") || name.contains("河南") || name.contains("山东") ||
                name.contains("陕西") || name.contains("天津")

    val isEnglishOrForeign: Boolean
        get() = language != "zh" || latitude.contains("英语") || latitude.contains("多语种")
}
