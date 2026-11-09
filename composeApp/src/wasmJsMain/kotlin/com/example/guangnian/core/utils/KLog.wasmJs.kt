package com.example.guangnian.core.utils

actual object KLog {
    actual fun d(tag: String, message: String) {
        println("[$tag] DEBUG: $message")
        LogCollector.addLog("D", tag, message)
    }

    actual fun i(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] INFO: $message")
        throwable?.printStackTrace()
        LogCollector.addLog("I", tag, message + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] WARN: $message")
        throwable?.printStackTrace()
        LogCollector.addLog("W", tag, message + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] ERROR: $message")
        throwable?.printStackTrace()
        LogCollector.addLog("E", tag, message + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
    }
}
