package com.example.guangnian.core.utils

actual object KLog {
    actual fun d(tag: String, message: String) {
        println("[$tag] DEBUG: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] ERROR: $message")
        throwable?.printStackTrace()
    }
}