package com.example.guangnian.core.utils

import android.util.Log

actual object KLog {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
        LogCollector.addLog("D", tag, message)
    }

    actual fun i(tag: String, message: String, throwable: Throwable?) {
        Log.i(tag, message, throwable)
        LogCollector.addLog("I", tag, message + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
        LogCollector.addLog("W", tag, message + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        LogCollector.addLog("E", tag, message + (throwable?.let { "\n${it.stackTraceToString()}" } ?: ""))
    }
}