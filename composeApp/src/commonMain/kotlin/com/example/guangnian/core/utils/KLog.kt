package com.example.guangnian.core.utils

expect object KLog {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}