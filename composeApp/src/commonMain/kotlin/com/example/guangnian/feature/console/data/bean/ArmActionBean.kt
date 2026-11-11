package com.example.guangnian.feature.console.data.bean

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

object ArmIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ArmId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) element.content else element.toString()
        } else {
            decoder.decodeString()
        }
    }
}

// 定义手臂动作列表响应实体（兼容整形ID与字符串ID）
@Serializable
data class ArmActionBean(
    @Serializable(with = ArmIdSerializer::class)
    val arm_id: String = "",
    @SerialName("arm_name")
    private val _armName: String? = null,
    @SerialName("name")
    private val _name: String? = null,
    @SerialName("time")
    private val _time: Double? = 0.0
) {
    val arm_name: String get() = _armName ?: _name ?: ""
    val name: String get() = arm_name
    val time: Double get() = _time ?: 0.0
}

