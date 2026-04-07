package com.example.guangnian

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.guangnian.core.network.tcp.TcpState
import com.example.guangnian.feature.robot.screen.MainControlContent

@Preview(
    device = "spec:width=896dp,height=414dp,orientation=landscape",
    showSystemUi = true,
    showBackground = true
)
@Composable
fun AppLandscapePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        MainControlContent(
            ip = "192.168.3.100",
            port = "8080",
            connectionState = TcpState.DISCONNECTED, // 你可以改成 CONNECTED 看看预览界面的变化！
            onIpChange = {},
            onPortChange = {},
            onConnectClick = {}
        )
    }
}