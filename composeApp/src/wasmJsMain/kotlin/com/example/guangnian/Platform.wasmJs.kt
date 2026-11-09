package com.example.guangnian

class WasmPlatform : Platform {
    override val name: String = "Web (WebAssembly)"
}

actual fun getPlatform(): Platform = WasmPlatform()
