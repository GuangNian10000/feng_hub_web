package com.example.guangnian.core.network.ws

/**
 * 手机端支持的通讯框架类型
 */
enum class CommunicationFramework(
    val displayName: String,
    val description: String,
    val defaultPort: Int,
    val defaultIp: String
) {
    /**
     * 新框架：通过原始 TCP 直连机器人主控服务端（默认端口 9002）
     */
    NEW_FRAMEWORK(
        displayName = "新框架",
        description = "TCP 直连机器人主控服务端 (ARM)",
        defaultPort = 9002,
        defaultIp = "192.168.1.100"
    ),

    /**
     * 老框架：通过 WebSocket 双通道连接平板端 feng_voice (默认端口 9090)
     */
    OLD_FRAMEWORK(
        displayName = "老框架",
        description = "WS 双通道连接平板端 (feng_voice)",
        defaultPort = 9090,
        defaultIp = "192.168.1.50"
    )
}
