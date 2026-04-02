package com.example.guangnian

import androidx.compose.runtime.Composable
// 注意：这里导入的是 Android 原生的 Preview 注解，功能更强大
import androidx.compose.ui.tooling.preview.Preview

// 关键点：使用 device 参数强制指定为横屏，并开启 showSystemUi 方便看边距
@Preview(
    device = "spec:width=896dp,height=414dp,orientation=landscape",
    showSystemUi = true,
    showBackground = true
)
@Composable
fun AppLandscapePreview() {
    // 直接调用你在 commonMain 里写好的跨平台 UI 入口
    App()
}