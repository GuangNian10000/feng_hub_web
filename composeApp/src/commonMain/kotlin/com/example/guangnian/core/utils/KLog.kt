package com.example.guangnian.core.utils

expect object KLog {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}