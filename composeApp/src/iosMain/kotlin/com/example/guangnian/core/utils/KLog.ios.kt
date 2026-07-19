package com.example.guangnian.core.utils

actual object KLog {
    actual fun d(tag: String, message: String) {
        println("[$tag] DEBUG: $message")
    }

    actual fun i(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] INFO: $message")
        throwable?.printStackTrace()
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] WARN: $message")
        throwable?.printStackTrace()
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] ERROR: $message")
        throwable?.printStackTrace()
    }
}
