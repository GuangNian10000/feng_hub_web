package com.example.guangnian.feature.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WebPortalAddressScreen(
    initialIp: String,
    initialPort: Int,
    onOpen: (ip: String, port: Int) -> Unit,
) {
    var ip by remember(initialIp) { mutableStateOf(initialIp) }
    var portText by remember(initialPort) { mutableStateOf(initialPort.toString()) }
    val parsedPort = portText.toIntOrNull()
    val ipIsValid = isValidIpv4(ip)
    val portIsValid = parsedPort != null && parsedPort in 1..65535

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080D18))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = Color(0xFF111A2B),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "峰客机器人 Web 控制",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "输入机器人 Web 地址，在 App 内横屏打开控制页面。",
                    color = Color(0xFF9BAAC0),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { value ->
                            if (value.length <= 15 && value.all { it.isDigit() || it == '.' }) ip = value
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("机器人 IP 地址") },
                        placeholder = { Text("192.168.123.164") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        colors = webFieldColors()
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() }) portText = value
                        },
                        modifier = Modifier.width(148.dp),
                        singleLine = true,
                        label = { Text("端口") },
                        placeholder = { Text("8080") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = webFieldColors()
                    )
                }

                Spacer(Modifier.height(8.dp))
                if (ip.isNotBlank() && !ipIsValid) {
                    Text("请输入有效的 IPv4 地址", color = Color(0xFFFF8A8A), fontSize = 12.sp)
                } else if (portText.isNotBlank() && !portIsValid) {
                    Text("端口范围为 1–65535", color = Color(0xFFFF8A8A), fontSize = 12.sp)
                } else {
                    Text(
                        text = if (ip.isNotBlank() && portText.isNotBlank()) "http://$ip:$portText/" else "",
                        color = Color(0xFF6EDBEA),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { parsedPort?.let { onOpen(ip.trim(), it) } },
                    enabled = ipIsValid && portIsValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF22D3E6),
                        contentColor = Color(0xFF04131A),
                        disabledBackgroundColor = Color(0xFF283447),
                        disabledContentColor = Color(0xFF718096)
                    )
                ) {
                    Text("打开机器人 Web", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "地址会保存在此设备，下次打开可直接使用或修改。",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.52f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun isValidIpv4(value: String): Boolean {
    val parts = value.trim().split('.')
    return parts.size == 4 && parts.all { part ->
        part.isNotEmpty() && part.length <= 3 && part.all { it.isDigit() } &&
            (part.toIntOrNull()?.let { it in 0..255 } == true)
    }
}

@Composable
private fun webFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = Color.White,
    backgroundColor = Color(0xFF0A1120),
    focusedBorderColor = Color(0xFF22D3E6),
    unfocusedBorderColor = Color(0xFF344258),
    focusedLabelColor = Color(0xFF22D3E6),
    unfocusedLabelColor = Color(0xFF94A3B8),
    cursorColor = Color(0xFF22D3E6)
)
