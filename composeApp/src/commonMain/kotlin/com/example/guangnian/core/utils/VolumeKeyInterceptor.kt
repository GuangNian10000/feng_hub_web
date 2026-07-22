package com.example.guangnian.core.utils

object VolumeKeyInterceptor {
    private var handler: ((Boolean) -> Unit)? = null

    fun register(handler: (Boolean) -> Unit) {
        this.handler = handler
    }

    fun unregister(handler: (Boolean) -> Unit) {
        if (this.handler === handler) {
            this.handler = null
        }
    }

    fun isEnabled(): Boolean = handler != null

    fun dispatch(increase: Boolean) {
        handler?.invoke(increase)
    }
}
