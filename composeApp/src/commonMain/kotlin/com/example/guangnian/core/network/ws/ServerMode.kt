package com.example.guangnian.core.network.ws

/**
 * 平板端连接服务端的通讯模式（对应老框架下的双通道行为模式）
 */
enum class ServerMode(val displayName: String, val description: String) {
    /**
     * 模式 A（当前默认模式）：
     * - 手机发给服务端 (ARM)：通过 /ws/proxy 纯透传通道，发送原始无包装 JSON (无 to/from/id)
     * - 手机发给平板自身 (TABLET)：通过 /ws/tablet 标准业务通道，携带 from/to/id
     * - 服务端回包不带 id，手机端按 type/topic 路由精准匹配并容错平铺字段
     */
    MODE_A(
        displayName = "模式 A (透传)",
        description = "透传通道 /ws/proxy (原始指令) + 业务通道 /ws/tablet"
    ),

    /**
     * 模式 B（全标准化网关模式）：
     * - 手机发给服务端 (to: 'arm') 或发给平板 (to: 'tablet') 统一走 /ws/tablet
     * - 全量携带 from: 'phone', to, id, type, topic
     * - 由平板统一网关根据 to 节点中继，通过唯一 id 精准完成异步挂起恢复
     */
    MODE_B(
        displayName = "模式 B (全网关)",
        description = "标准业务网关通道 /ws/tablet (全量 to/from/id 封装)"
    )
}
