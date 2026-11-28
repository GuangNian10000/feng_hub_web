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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    var showTopBar by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080D18))) {
            if (showTopBar) {
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
                    PortalButton("隐藏顶栏", onClick = {
                        showTopBar = false
                    })
                }
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

                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = pageProgress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter),
                        color = Color(0xFF22D3E6),
                        backgroundColor = Color(0xFF172438)
                    )
                }

                pageError?.let { message ->
                    Card(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        backgroundColor = Color(0xF0111A2B),
                        elevation = 8.dp
                    ) {
                        Column(
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

                DraggableFloatingMenuButton(
                    urlText = "${target.first}:${target.second}",
                    canGoBack = webView?.canGoBack() == true,
                    showTopBar = showTopBar,
                    onRefresh = {
                        pageError = null
                        webView?.reload()
                    },
                    onChangeAddress = {
                        pageError = null
                        endpoint = null
                    },
                    onGoBack = {
                        if (webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            endpoint = null
                        }
                    },
                    onToggleTopBar = {
                        showTopBar = !showTopBar
                    }
                )
            }
        }
    }
}

@Composable
private fun DraggableFloatingMenuButton(
    urlText: String,
    canGoBack: Boolean,
    showTopBar: Boolean,
    onRefresh: () -> Unit,
    onChangeAddress: () -> Unit,
    onGoBack: () -> Unit,
    onToggleTopBar: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val buttonSizeDp = 48.dp
        val buttonSizePx = with(density) { buttonSizeDp.toPx() }

        var currentX by remember { mutableStateOf(-1f) }
        var currentY by remember { mutableStateOf(-1f) }
        var targetX by remember { mutableStateOf(-1f) }
        var targetY by remember { mutableStateOf(-1f) }
        var isDocked by remember { mutableStateOf(true) }
        var dockSideIsLeft by remember { mutableStateOf(true) } // 默认初始化在左边
        var isDragging by remember { mutableStateOf(false) }
        var showMenuDialog by remember { mutableStateOf(false) }
        var lastInteractionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

        // 默认初始化位置在左侧吸附收起位置
        if (currentX < 0f && containerWidth > 0f) {
            val initialDockX = -buttonSizePx / 2f
            val initialY = (containerHeight * 0.35f).coerceIn(0f, maxOf(0f, containerHeight - buttonSizePx))
            currentX = initialDockX
            targetX = initialDockX
            currentY = initialY
            targetY = initialY
            dockSideIsLeft = true
            isDocked = true
        }

        if (containerWidth > 0f && containerHeight > 0f) {
            val clampedY = targetY.coerceIn(0f, maxOf(0f, containerHeight - buttonSizePx))
            if (clampedY != targetY) {
                targetY = clampedY
                currentY = clampedY
            }
            if (isDocked) {
                val clampedX = if (dockSideIsLeft) -buttonSizePx / 2f else containerWidth - buttonSizePx / 2f
                if (clampedX != targetX) {
                    targetX = clampedX
                    currentX = clampedX
                }
            }
        }

        // 3秒无操作自动吸附收起（弹窗关闭后或放开在中间无操作后 3 秒触发）
        LaunchedEffect(isDragging, showMenuDialog, isDocked, lastInteractionTimestamp) {
            if (!isDragging && !showMenuDialog && !isDocked) {
                delay(3000)
                val isLeft = (currentX + buttonSizePx / 2f) < (containerWidth / 2f)
                dockSideIsLeft = isLeft
                targetX = if (isLeft) -buttonSizePx / 2f else containerWidth - buttonSizePx / 2f
                targetY = currentY.coerceIn(0f, maxOf(0f, containerHeight - buttonSizePx))
                isDocked = true
                currentX = targetX
            }
        }

        val animX by animateFloatAsState(
            targetValue = if (isDragging) currentX else targetX,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
        val animY by animateFloatAsState(
            targetValue = if (isDragging) currentY else targetY,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )

        val targetAlpha = when {
            isDragging || showMenuDialog -> 1.0f
            isDocked -> 0.4f
            else -> 1.0f
        }
        val animAlpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 300)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(animX.roundToInt(), animY.roundToInt()) }
                .graphicsLayer { alpha = animAlpha }
                .size(buttonSizeDp)
                .clip(CircleShape)
                .background(Color(0xEE111A2B))
                .border(1.5.dp, Color(0xFF22D3E6), CircleShape)
                .pointerInput(containerWidth, containerHeight) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            val pointerId = down.id
                            var dragDistance = 0f
                            isDragging = true

                            var isClick = true
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    break
                                }
                                val dragAmount = change.positionChange()
                                if (dragAmount.getDistance() > 0.5f) {
                                    change.consume()
                                    dragDistance += dragAmount.getDistance()
                                    if (dragDistance > touchSlop) {
                                        isClick = false
                                    }
                                    currentX += dragAmount.x
                                    currentY = (currentY + dragAmount.y).coerceIn(0f, maxOf(0f, containerHeight - buttonSizePx))
                                }
                            }

                            isDragging = false
                            lastInteractionTimestamp = System.currentTimeMillis()
                            val duration = System.currentTimeMillis() - downTime

                            if (isClick && dragDistance < touchSlop && duration < 500) {
                                // 点击悬浮球：高亮展开并弹出菜单
                                if (isDocked) {
                                    isDocked = false
                                    targetX = if (dockSideIsLeft) 8.dp.toPx() else containerWidth - buttonSizePx - 8.dp.toPx()
                                } else {
                                    targetX = currentX
                                }
                                targetY = currentY
                                showMenuDialog = true
                            } else {
                                // 拖拽释放：只有靠近屏幕边缘才立刻自动吸附
                                val edgeThreshold = maxOf(80.dp.toPx(), containerWidth * 0.18f)
                                val distToLeft = currentX + buttonSizePx / 2f
                                val distToRight = containerWidth - (currentX + buttonSizePx / 2f)

                                if (distToLeft <= edgeThreshold) {
                                    dockSideIsLeft = true
                                    targetX = -buttonSizePx / 2f
                                    targetY = currentY
                                    isDocked = true
                                } else if (distToRight <= edgeThreshold) {
                                    dockSideIsLeft = false
                                    targetX = containerWidth - buttonSizePx / 2f
                                    targetY = currentY
                                    isDocked = true
                                } else {
                                    // 在屏幕中间区域释放：保持完全显示，不立刻吸附（交由 3秒定时器无操作后收起）
                                    targetX = currentX.coerceIn(8.dp.toPx(), containerWidth - buttonSizePx - 8.dp.toPx())
                                    targetY = currentY
                                    isDocked = false
                                }
                            }
                            currentX = targetX
                            currentY = targetY
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "悬浮菜单",
                tint = Color(0xFF22D3E6),
                modifier = Modifier.size(24.dp)
            )
        }

        if (showMenuDialog) {
            FloatingMenuDialog(
                urlText = urlText,
                canGoBack = canGoBack,
                showTopBar = showTopBar,
                onDismiss = {
                    showMenuDialog = false
                    lastInteractionTimestamp = System.currentTimeMillis()
                },
                onRefresh = onRefresh,
                onChangeAddress = onChangeAddress,
                onGoBack = onGoBack,
                onToggleTopBar = onToggleTopBar
            )
        }
    }
}

@Composable
private fun FloatingMenuDialog(
    urlText: String,
    canGoBack: Boolean,
    showTopBar: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onChangeAddress: () -> Unit,
    onGoBack: () -> Unit,
    onToggleTopBar: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF111A2B),
            contentColor = Color.White,
            elevation = 12.dp,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF19263A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFF22D3E6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Web 控制菜单",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = urlText,
                                color = Color(0xFF9BAAC0),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF9BAAC0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Divider(color = Color(0xFF1F2D44), thickness = 1.dp)

                FloatingMenuItem(
                    icon = Icons.Default.Refresh,
                    title = "刷新页面",
                    subtitle = "重新加载当前网页内容",
                    onClick = {
                        onDismiss()
                        onRefresh()
                    }
                )

                FloatingMenuItem(
                    icon = Icons.Default.Edit,
                    title = "更换地址",
                    subtitle = "重新设置机器人 IP 与端口",
                    onClick = {
                        onDismiss()
                        onChangeAddress()
                    }
                )

                if (canGoBack) {
                    FloatingMenuItem(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        title = "返回上一页",
                        subtitle = "退回 Web 浏览上一页",
                        onClick = {
                            onDismiss()
                            onGoBack()
                        }
                    )
                }

                FloatingMenuItem(
                    icon = Icons.Default.Menu,
                    title = if (showTopBar) "隐藏顶栏" else "显示顶栏",
                    subtitle = if (showTopBar) "隐藏页面顶部控制栏" else "在页面顶部显示控制栏",
                    onClick = {
                        onDismiss()
                        onToggleTopBar()
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatingMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF172438))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF22D3E6).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF22D3E6),
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFF8FA0B8),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PortalButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
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
