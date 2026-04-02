package com.example.guangnian

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform