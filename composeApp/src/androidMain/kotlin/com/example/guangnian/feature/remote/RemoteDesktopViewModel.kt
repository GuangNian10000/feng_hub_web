package com.example.guangnian.feature.remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── 连接状态 ─────────────────────────────────────────────────────────────────

enum class RemoteConnectState {
    IDLE,        // 未开始
    CONNECTING,  // 连接中（ADB + 服务器部署）
    CONNECTED,   // 连接成功（镜像界面）
    FAILED       // 连接失败
}

// ─── 分辨率选项（对应 ScrcpyForAndroid options_resolution_values）────────────

data class ResolutionOption(val label: String, val width: Int, val height: Int)

val RESOLUTION_OPTIONS = listOf(
    ResolutionOption("720×1280",  720,  1280),
    ResolutionOption("1080×1920", 1080, 1920),
    ResolutionOption("1080×2340", 1080, 2340),
    ResolutionOption("1080×2400", 1080, 2400),
    ResolutionOption("1440×2560", 1440, 2560),
)

// ─── 码率选项（对应 options_bitrate_keys / options_bitrate_values）─────────

data class BitrateOption(val label: String, val bps: Int)

val BITRATE_OPTIONS = listOf(
    BitrateOption("1 Mbps",  1_000_000),
    BitrateOption("2 Mbps",  2_000_000),
    BitrateOption("4 Mbps",  4_000_000),
    BitrateOption("8 Mbps",  8_000_000),
    BitrateOption("16 Mbps", 16_000_000),
)

// ─── 延迟选项（对应 options_delay_keys / options_delay_values）───────────────

data class DelayOption(val label: String, val ms: Int)

val DELAY_OPTIONS = listOf(
    DelayOption("0 ms",   0),
    DelayOption("50 ms",  50),
    DelayOption("100 ms", 100),
    DelayOption("200 ms", 200),
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

/**
 * 使用 AndroidViewModel 以便在 viewModelScope 中直接访问 Application Context，
 * 避免因 Composition 离开导致 rememberCoroutineScope 失效的问题。
 */
class RemoteDesktopViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()

    // 配置参数
    private val _ip = MutableStateFlow("")
    val ip: StateFlow<String> = _ip.asStateFlow()

    private val _resolution = MutableStateFlow(RESOLUTION_OPTIONS[1])
    val resolution: StateFlow<ResolutionOption> = _resolution.asStateFlow()

    private val _bitrate = MutableStateFlow(BITRATE_OPTIONS[0])   // 默认 1Mbps
    val bitrate: StateFlow<BitrateOption> = _bitrate.asStateFlow()

    private val _delay = MutableStateFlow(DELAY_OPTIONS[3])        // 默认 200ms
    val delay: StateFlow<DelayOption> = _delay.asStateFlow()

    private val _noControl = MutableStateFlow(false)
    val noControl: StateFlow<Boolean> = _noControl.asStateFlow()

    private val _showNav = MutableStateFlow(false)
    val showNav: StateFlow<Boolean> = _showNav.asStateFlow()

    private val _screenOff = MutableStateFlow(false)
    val screenOff: StateFlow<Boolean> = _screenOff.asStateFlow()

    private val _audioForward = MutableStateFlow(false)
    val audioForward: StateFlow<Boolean> = _audioForward.asStateFlow()

    // 连接状态
    private val _connectState = MutableStateFlow(RemoteConnectState.IDLE)
    val connectState: StateFlow<RemoteConnectState> = _connectState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ─── 用户操作 ─────────────────────────────────────────────────────────

    fun setIp(value: String) { _ip.value = value }
    fun setResolution(value: ResolutionOption) { _resolution.value = value }
    fun setBitrate(value: BitrateOption) { _bitrate.value = value }
    fun setDelay(value: DelayOption) { _delay.value = value }
    fun setNoControl(value: Boolean) { _noControl.value = value }
    fun setShowNav(value: Boolean) { _showNav.value = value }
    fun setScreenOff(value: Boolean) { _screenOff.value = value }
    fun setAudioForward(value: Boolean) { _audioForward.value = value }

    fun resetToIdle() {
        _connectState.value = RemoteConnectState.IDLE
        _errorMessage.value = null
    }

    // ─── 连接逻辑（在 viewModelScope 中执行，不依赖 Composition 生命周期）──

    fun startConnection() {
        if (_connectState.value == RemoteConnectState.CONNECTING) return
        _connectState.value = RemoteConnectState.CONNECTING
        _errorMessage.value = null

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // 1. 启动 ADB server
                    ScrcpyEngine.startAdbServer(app)
                    Thread.sleep(500)

                    // 2. 提取 jar
                    val serverJar = ScrcpyEngine.extractServerJar(app)
                    if (serverJar == null) {
                        withContext(Dispatchers.Main) {
                            _connectState.value = RemoteConnectState.FAILED
                            _errorMessage.value = "无法提取 scrcpy-server.jar"
                        }
                        return@withContext
                    }

                    val rawIp = _ip.value.trim()
                    val adbPort = if (rawIp.contains(":"))
                        rawIp.substringAfterLast(":").toIntOrNull() ?: 5555 else 5555
                    val host = if (rawIp.contains(":")) rawIp.substringBeforeLast(":") else rawIp
                    val localIp = getLocalIpAddress() ?: "127.0.0.1"

                    val result = ScrcpyEngine.connectAndStartServer(
                        context = app,
                        serverJar = serverJar,
                        ip = host,
                        adbPort = adbPort,
                        forwardPort = ScrcpyService.LOCAL_FORWARD_PORT,
                        controlPort = ScrcpyService.LOCAL_CONTROL_PORT,
                        bitrate = _bitrate.value.bps,
                        maxSize = maxOf(_resolution.value.width, _resolution.value.height),
                        audioEnabled = _audioForward.value,
                        localIp = localIp
                    )

                    withContext(Dispatchers.Main) {
                        if (result == 0) {
                            _connectState.value = RemoteConnectState.CONNECTED
                        } else {
                            _connectState.value = RemoteConnectState.FAILED
                            _errorMessage.value = "连接失败（错误码 $result）\n请确认目标设备已开启 ADB 无线调试"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _connectState.value = RemoteConnectState.FAILED
                        _errorMessage.value = "连接异常: ${e.message}"
                    }
                }
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                val addresses = ni.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }
}
