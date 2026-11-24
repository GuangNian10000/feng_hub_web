package com.example.guangnian.feature.web

import android.graphics.Bitmap
import android.net.Uri
import android.content.Context
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun RobotWebPortalApp(
    initialIp: String,
    initialPort: Int,
    onConnect: (ip: String, port: Int) -> Unit,
    onChooseFiles: (WebChromeClient.FileChooserParams, android.webkit.ValueCallback<Array<Uri>>) -> Boolean,
    onWebPermissionRequest: (PermissionRequest) -> Unit,
    onWebPermissionCancelled: (PermissionRequest) -> Unit,
) {
    var endpoint by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var lastIp by remember(initialIp) { mutableStateOf(initialIp) }
    var lastPort by remember(initialPort) { mutableStateOf(initialPort) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableStateOf(0) }
    var pageError by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = endpoint != null) {
        val current = webView
        if (current?.canGoBack() == true) current.goBack() else endpoint = null
    }

    val target = endpoint
    if (target == null) {
        WebPortalAddressScreen(lastIp, lastPort) { ip, port ->
            onConnect(ip, port)
            lastIp = ip
            lastPort = port
            pageError = null
            endpoint = ip to port
        }
    } else {
        val url = "http://${target.first}:${target.second}/"
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080D18))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF111A2B))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PortalButton("‹ 返回", onClick = {
                    if (webView?.canGoBack() == true) webView?.goBack() else endpoint = null
                })
                Text(
                    text = "${target.first}:${target.second}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFB8C6D9),
                    fontSize = 13.sp,
                    maxLines = 1
                )
                PortalButton("刷新", onClick = {
                    pageError = null
                    webView?.reload()
                })
                PortalButton("换地址", onClick = {
                    pageError = null
                    endpoint = null
                })
            }
            if (pageProgress in 1..99) {
                LinearProgressIndicator(
                    progress = pageProgress / 100f,
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF22D3E6),
                    backgroundColor = Color(0xFF172438)
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        createRobotWebView(
                            context = viewContext,
                            onProgress = { pageProgress = it },
                            onMainFrameError = { pageError = it },
                            onChooseFiles = onChooseFiles,
                            onWebPermissionRequest = onWebPermissionRequest,
                            onWebPermissionCancelled = onWebPermissionCancelled,
                        ).also {
                            webView = it
                            it.loadUrl(url)
                        }
                    },
                    update = { view ->
                        webView = view
                        if (view.url != url) view.loadUrl(url)
                    },
                    onRelease = { released ->
                        if (webView === released) webView = null
                        released.stopLoading()
                        released.webChromeClient = null
                        released.destroy()
                    },
                )
                pageError?.let { message ->
                    androidx.compose.material.Card(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        backgroundColor = Color(0xF0111A2B),
                        elevation = 8.dp
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("网页暂时无法打开", color = Color.White, fontSize = 17.sp)
                            Text(message, color = Color(0xFF9BAAC0), fontSize = 12.sp)
                            PortalButton("重试", onClick = {
                                pageError = null
                                webView?.loadUrl(url)
                            })
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun PortalButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF19263A),
            contentColor = Color(0xFF66E7F2)
        ),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

private fun createRobotWebView(
    context: Context,
    onProgress: (Int) -> Unit,
    onMainFrameError: (String?) -> Unit,
    onChooseFiles: (WebChromeClient.FileChooserParams, android.webkit.ValueCallback<Array<Uri>>) -> Boolean,
    onWebPermissionRequest: (PermissionRequest) -> Unit,
    onWebPermissionCancelled: (PermissionRequest) -> Unit,
): WebView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.setSupportMultipleWindows(false)
    settings.allowFileAccess = false
    settings.allowContentAccess = true
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onMainFrameError(null)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                onMainFrameError(error?.description?.toString() ?: "请检查机器人 IP、端口和网络连接")
            }
        }
    }
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgress(newProgress)
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (filePathCallback == null || fileChooserParams == null) return false
            return onChooseFiles(fileChooserParams, filePathCallback)
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            if (request == null) return
            onWebPermissionRequest(request)
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            if (request != null) onWebPermissionCancelled(request)
        }
    }
}
