package com.example.guangnian.feature.remote

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.math.BigInteger
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean

// ─── 协议常量（文件级别，所有方法都可访问）──────────────────────────────────

private const val VIDEO_HEAD_LEN = 10
private const val PACKET_TYPE_VIDEO = 1
private const val PACKET_TYPE_AUDIO = 0
private const val FLAG_FRAME = 0
private const val FLAG_KEY_FRAME = 1
private const val FLAG_CONFIG = 2
private const val FLAG_END = 4
private const val BACKGROUND_SLEEP_MS = 30L

/**
 * Scrcpy TCP 流服务，移植自 ScrcpyForAndroid / Scrcpy.java
 *
 * 负责：
 *  - 连接远程设备的视频流端口（7007）与控制通道（7008）
 *  - 解码 H.264 视频帧并渲染到 Surface
 *  - 解码 AAC 音频并播放
 *  - 将触控事件、按键事件序列化发送给控制通道
 */
class ScrcpyService : Service() {

    companion object {
        private const val TAG = "ScrcpyService"
        const val LOCAL_IP = "127.0.0.1"
        const val LOCAL_FORWARD_PORT = 7007
        const val LOCAL_CONTROL_PORT = 7008
        const val DEFAULT_ADB_PORT = 5555

        // 特殊 keycode（与 server 端约定）
        const val KEYCODE_SCREEN_OFF = 999
        const val KEYCODE_DISPLAY_POWER_TOGGLE = 1000
        const val KEYCODE_STREAM_PAUSE = 1001
        const val KEYCODE_STREAM_RESUME = 1002
    }

    // ─── 状态 ────────────────────────────────────────────────────────────────

    private var serverHost: String = LOCAL_IP
    private var serverPort: Int = DEFAULT_ADB_PORT
    private var surface: Surface? = null
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var audioEnabled: Boolean = true

    private val eventQueue: Queue<ByteArray> = LinkedList()
    private var videoDecoder: VideoDecoder? = null
    private var audioDecoder: AudioDecoder? = null
    private var mediaSocket: Socket? = null
    private var controlSocket: Socket? = null
    
    private val updateAvailable = AtomicBoolean(false)
    private val letServiceRunning = AtomicBoolean(true)
    private val mBinder = LocalBinder()

    private var firstTime: Boolean = true
    private var socketStatus: Boolean = false
    private var backgroundMode: Boolean = false
    private var pendingForegroundRefresh: Boolean = false
    private var cachedStreamSettings: CachedStreamSettings? = null
    private var cachedKeyFrame: ByteArray? = null
    private var suspendStream: Boolean = false

    val remoteDevResolution = IntArray(2)
    var serviceCallbacks: ServiceCallbacks? = null

    // ─── Binder ──────────────────────────────────────────────────────────────

    inner class LocalBinder : Binder() {
        fun getService(): ScrcpyService = this@ScrcpyService
    }

    override fun onBind(intent: Intent?): IBinder = mBinder

    // ─── 公开接口 ────────────────────────────────────────────────────────────

    fun start(
        surface: Surface,
        serverAddr: String,
        screenHeight: Int,
        screenWidth: Int,
        delay: Int,
        audioEnabled: Boolean
    ) {
        this.audioEnabled = audioEnabled
        this.videoDecoder = VideoDecoder().also { it.start() }
        this.audioDecoder = if (audioEnabled) AudioDecoder().also { it.start() } else null

        val (host, port) = parseAddr(serverAddr)
        this.serverHost = host
        this.serverPort = port
        this.screenHeight = screenHeight
        this.screenWidth = screenWidth
        this.surface = surface

        Thread { startConnection(serverHost, serverPort, delay) }.start()
    }

    fun setParms(surface: Surface, width: Int, height: Int) {
        this.screenWidth = width
        this.screenHeight = height
        this.surface = surface
        videoDecoder?.start()
        if (audioEnabled) audioDecoder?.start()
        updateAvailable.set(true)
    }

    fun pause() {
        videoDecoder?.stop()
        audioDecoder?.stop()
    }

    fun resume() {
        videoDecoder?.start()
        audioDecoder?.start()
        updateAvailable.set(true)
    }

    fun setBackgroundMode(enable: Boolean) {
        backgroundMode = enable
        if (enable) {
            pause()
        } else {
            resume()
            pendingForegroundRefresh = true
            updateAvailable.set(true)
        }
    }

    fun setStreamSuspended(enable: Boolean) { suspendStream = enable }

    fun stopService() {
        letServiceRunning.set(false)
        videoDecoder?.stop()
        audioDecoder?.stop()
        runCatching { mediaSocket?.close() }
        runCatching { controlSocket?.close() }
        stopSelf()
    }

    fun checkSocketConnection(): Boolean = socketStatus
    fun getRemoteDeviceResolution(): IntArray = remoteDevResolution

    fun touchEvent(event: MotionEvent, landscape: Boolean, displayW: Int, displayH: Int): Boolean {
        val remoteW: Float
        val remoteH: Float
        val realH: Float
        val realW: Float

        if (landscape) {
            remoteW = maxOf(remoteDevResolution[0], remoteDevResolution[1]).toFloat()
            remoteH = minOf(remoteDevResolution[0], remoteDevResolution[1]).toFloat()
            realW = minOf(remoteW, screenWidth.toFloat())
            realH = realW * remoteH / remoteW
        } else {
            remoteW = minOf(remoteDevResolution[0], remoteDevResolution[1]).toFloat()
            remoteH = maxOf(remoteDevResolution[0], remoteDevResolution[1]).toFloat()
            realH = minOf(remoteH, screenHeight.toFloat())
            realW = realH * remoteW / remoteH
        }

        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)
        val actionMasked = event.actionMasked

        when (actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    sendTouchEventInternal(
                        MotionEvent.ACTION_MOVE, event.buttonState,
                        (event.getX(i) * realW / displayW).toInt(),
                        (event.getY(i) * realH / displayH).toInt(),
                        event.getPointerId(i)
                    )
                }
            }
            else -> sendTouchEventInternal(
                event.action, event.buttonState,
                (event.getX(actionIndex) * realW / displayW).toInt(),
                (event.getY(actionIndex) * realH / displayH).toInt(),
                pointerId
            )
        }
        return true
    }

    fun sendKeyEvent(keycode: Int) = sendKeyEventWithAction(keycode, 0, 0)

    fun sendKeyEventWithAction(keycode: Int, action: Int, repeat: Int) {
        val actionRepeat = (repeat shl 8) or (action and 0xFF)
        enqueueEvent(intArrayOf(keycode, actionRepeat, 0, 0, 0))
    }

    fun sendScreenOff() = sendKeyEvent(KEYCODE_SCREEN_OFF)
    fun sendDisplayPowerToggle() = sendKeyEvent(KEYCODE_DISPLAY_POWER_TOGGLE)
    fun sendStreamPause() = sendKeyEvent(KEYCODE_STREAM_PAUSE)
    fun sendStreamResume() = sendKeyEvent(KEYCODE_STREAM_RESUME)

    // ─── 内部实现 ────────────────────────────────────────────────────────────

    private fun sendTouchEventInternal(action: Int, buttonState: Int, x: Int, y: Int, pointerId: Int) {
        enqueueEvent(intArrayOf(action, buttonState, x, y, pointerId))
    }

    private fun enqueueEvent(buf: IntArray) {
        if (!letServiceRunning.get()) return
        val array = ByteArray(buf.size * 4)
        for (j in buf.indices) {
            val c = buf[j]
            array[j * 4]     = ((c and -0x1000000) ushr 24).toByte()
            array[j * 4 + 1] = ((c and 0x00FF0000) ushr 16).toByte()
            array[j * 4 + 2] = ((c and 0x0000FF00) ushr 8).toByte()
            array[j * 4 + 3] = (c and 0x000000FF).toByte()
        }
        eventQueue.offer(array)
    }

    private fun parseAddr(addr: String): Pair<String, Int> {
        return when {
            addr.startsWith("[") -> {
                val host = addr.substringAfter("[").substringBefore("]")
                val port = addr.substringAfter("]:").toIntOrNull() ?: DEFAULT_ADB_PORT
                host to port
            }
            addr.contains(":") -> {
                val host = addr.substringBeforeLast(":")
                val port = addr.substringAfterLast(":").toIntOrNull() ?: DEFAULT_ADB_PORT
                host to port
            }
            else -> addr to DEFAULT_ADB_PORT
        }
    }

    private fun startConnection(ip: String, port: Int, delay: Int) {
        videoDecoder = VideoDecoder().also { it.start() }
        audioDecoder = if (audioEnabled) AudioDecoder().also { it.start() } else null

        var mediaInputStream: DataInputStream? = null
        var controlOutputStream: DataOutputStream? = null
        var firstConnect = true
        var attempts = 50
        val controlPort = port + 1
        var consecutiveRefused = 0

        while (attempts > 0) {
            try {
                mediaSocket = Socket().apply { connect(InetSocketAddress(ip, port), 5000) }
                controlSocket = Socket().apply { connect(InetSocketAddress(ip, controlPort), 5000) }

                if (!letServiceRunning.get()) return

                consecutiveRefused = 0
                if (firstConnect) { firstConnect = false; attempts = 5 }

                mediaSocket?.soTimeout = 5000
                mediaInputStream = DataInputStream(mediaSocket?.getInputStream()!!)
                controlOutputStream = DataOutputStream(controlSocket?.getOutputStream()!!)

                // 读取 8 字节分辨率（宽、高各 4 字节）
                val buf = ByteArray(8)
                var bytesRead = 0
                while (bytesRead < 8) {
                    val n = mediaInputStream.read(buf, bytesRead, 8 - bytesRead)
                    if (n == -1) throw IOException("Connection closed reading resolution")
                    bytesRead += n
                }
                mediaSocket?.soTimeout = 0
                attempts = 0

                for (i in remoteDevResolution.indices) {
                    remoteDevResolution[i] = ((buf[i * 4].toInt() and 0xFF) shl 24) or
                            ((buf[i * 4 + 1].toInt() and 0xFF) shl 16) or
                            ((buf[i * 4 + 2].toInt() and 0xFF) shl 8) or
                            (buf[i * 4 + 3].toInt() and 0xFF)
                }
                if (remoteDevResolution[0] > remoteDevResolution[1]) {
                    val tmp = remoteDevResolution[0]
                    remoteDevResolution[0] = remoteDevResolution[1]
                    remoteDevResolution[1] = tmp
                }
                socketStatus = true

                loop(mediaInputStream, controlOutputStream, delay)
            } catch (e: Exception) {
                e.printStackTrace()
                if (letServiceRunning.get()) {
                    val isRefused = (e is ConnectException) ||
                            (e.message?.contains("ECONNREFUSED") == true) ||
                            (e.message?.contains("Connection refused") == true)
                    if (isRefused) {
                        consecutiveRefused++
                        if (consecutiveRefused >= 3) {
                            socketStatus = false
                            serviceCallbacks?.onErrorDisconnect()
                            return
                        }
                    }
                    attempts--
                    if (attempts < 0) {
                        socketStatus = false
                        serviceCallbacks?.onErrorDisconnect()
                        return
                    }
                    try { Thread.sleep(if (isRefused) 500L else 100L) } catch (_: InterruptedException) {}
                }
            } finally {
                controlSocket?.runCatching { close() }
                mediaSocket?.runCatching { close() }
                controlOutputStream?.runCatching { close() }
                mediaInputStream?.runCatching { close() }
                eventQueue.clear()
            }
        }
    }

    @Throws(InterruptedException::class)
    private fun loop(mediaInput: DataInputStream, controlOutput: DataOutputStream, delay: Int) {
        var streamSettings: CachedStreamSettings? = null
        val packetSizeBuf = ByteArray(4)
        var lastVideoOffset = 0L
        var lastAudioOffset = 0L

        while (letServiceRunning.get()) {
            var waitEvent = true
            try {
                // 从后台切回前景时，刷新解码器
                if (!backgroundMode && pendingForegroundRefresh) {
                    val s = surface
                    if (s == null || !s.isValid) { Thread.sleep(5); continue }
                    cachedStreamSettings?.let { css ->
                        streamSettings = css
                        videoDecoder?.configure(s, screenWidth, screenHeight, css.sps.duplicate(), css.pps.duplicate())
                        cachedKeyFrame?.let { kf ->
                            val flag = kf[1].toInt() and 0xFF
                            videoDecoder?.decodeSample(kf, VIDEO_HEAD_LEN, kf.size - VIDEO_HEAD_LEN, 0L, flag)
                        }
                    }
                    pendingForegroundRefresh = false
                }

                // 发送控制事件
                val sendEvt = eventQueue.poll()
                if (sendEvt != null) {
                    waitEvent = false
                    try {
                        controlOutput.write(sendEvt, 0, sendEvt.size)
                        controlOutput.flush()
                    } catch (e: IOException) {
                        Log.e(TAG, "Control write failed", e)
                        serviceCallbacks?.onErrorDisconnect()
                        letServiceRunning.set(false)
                    }
                }

                if (suspendStream) { Thread.sleep(50); continue }

                // 读取媒体包
                if (mediaInput.available() > 0) {
                    waitEvent = false
                    mediaInput.readFully(packetSizeBuf, 0, 4)
                    val size = packetBytesToInt(packetSizeBuf)
                    if (size <= 0 || size > 4 * 1024 * 1024) {
                        Log.e(TAG, "Invalid packet size=$size")
                        serviceCallbacks?.onErrorDisconnect()
                        letServiceRunning.set(false)
                        return
                    }

                    val packet = ByteArray(size)
                    mediaInput.readFully(packet, 0, size)

                    when (packet[0].toInt() and 0xFF) {
                        PACKET_TYPE_VIDEO -> {
                            lastVideoOffset = handleVideoPacket(packet, delay, streamSettings, lastVideoOffset) { css ->
                                streamSettings = css
                            }
                        }
                        PACKET_TYPE_AUDIO -> {
                            lastAudioOffset = handleAudioPacket(packet, delay, lastAudioOffset)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Loop IOException: ${e.message}")
            } finally {
                if (waitEvent) Thread.sleep(5)
            }
        }
    }

    private fun handleVideoPacket(
        packet: ByteArray,
        delay: Int,
        currentStreamSettings: CachedStreamSettings?,
        lastVideoOffset: Long,
        onNewSettings: (CachedStreamSettings) -> Unit
    ): Long {
        var videoOffset = lastVideoOffset
        val flag = packet[1].toInt() and 0xFF
        val ts = packetBytesToLong(packet, 2)

        when (flag) {
            FLAG_CONFIG -> {
                if (backgroundMode) {
                    val data = packet.copyOfRange(VIDEO_HEAD_LEN, packet.size)
                    parseStreamSettings(data)?.let { cachedStreamSettings = it }
                    Thread.sleep(BACKGROUND_SLEEP_MS)
                    return videoOffset
                }
                val data = packet.copyOfRange(VIDEO_HEAD_LEN, packet.size)
                parseStreamSettings(data)?.let { css ->
                    onNewSettings(css)
                    if (!firstTime) {
                        serviceCallbacks?.onLoadNewRotation()
                    }
                    updateAvailable.set(false)
                    surface?.takeIf { it.isValid }?.let { s ->
                        videoDecoder?.configure(s, screenWidth, screenHeight, css.sps.duplicate(), css.pps.duplicate())
                    }
                }
            }
            FLAG_KEY_FRAME -> {
                if (backgroundMode) {
                    cachedKeyFrame = packet
                    Thread.sleep(BACKGROUND_SLEEP_MS)
                    return videoOffset
                }
                videoDecoder?.decodeSample(packet, VIDEO_HEAD_LEN, packet.size - VIDEO_HEAD_LEN, 0L, flag)
            }
            FLAG_END -> Log.e(TAG, "VIDEO END")
            else -> { // FLAG_FRAME
                if (backgroundMode) { Thread.sleep(BACKGROUND_SLEEP_MS); return videoOffset }
                if (videoOffset == 0L) videoOffset = System.currentTimeMillis() - ts / 1000
                videoDecoder?.decodeSample(packet, VIDEO_HEAD_LEN, packet.size - VIDEO_HEAD_LEN, 0L, flag)
            }
        }
        firstTime = false
        return videoOffset
    }

    private fun handleAudioPacket(packet: ByteArray, delay: Int, lastAudioOffset: Long): Long {
        if (!audioEnabled || audioDecoder == null) return lastAudioOffset
        var audioOffset = lastAudioOffset
        val flag = packet[1].toInt() and 0xFF
        val ts = packetBytesToLong(packet, 2)

        when (flag) {
            FLAG_CONFIG -> audioDecoder?.configure(packet.copyOfRange(VIDEO_HEAD_LEN, packet.size))
            FLAG_END -> Log.e(TAG, "AUDIO END")
            else -> {
                if (audioOffset == 0L) audioOffset = System.currentTimeMillis() - ts / 1000
                if (System.currentTimeMillis() - (audioOffset + ts / 1000) < delay) {
                    audioDecoder?.decodeSample(packet, VIDEO_HEAD_LEN, packet.size - VIDEO_HEAD_LEN, 0L, flag)
                }
            }
        }
        return audioOffset
    }

    // ─── 协议工具 ────────────────────────────────────────────────────────────

    private data class CachedStreamSettings(val sps: ByteBuffer, val pps: ByteBuffer)

    private fun parseStreamSettings(data: ByteArray): CachedStreamSettings? {
        if (data.size < 8) return null
        var ppsIndex = -1
        for (i in 4..data.size - 4) {
            if (data[i] == 0x00.toByte() && data[i + 1] == 0x00.toByte() &&
                data[i + 2] == 0x00.toByte() && data[i + 3] == 0x01.toByte()
            ) { ppsIndex = i; break }
        }
        if (ppsIndex <= 0) { Log.w(TAG, "Missing PPS start code, len=${data.size}"); return null }
        val sps = ByteBuffer.wrap(data.copyOfRange(0, ppsIndex))
        val pps = ByteBuffer.wrap(data.copyOfRange(ppsIndex, data.size))
        return CachedStreamSettings(sps, pps)
    }

    private fun packetBytesToInt(b: ByteArray): Int =
        ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or
                ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)

    private fun packetBytesToLong(b: ByteArray, offset: Int): Long =
        BigInteger(b.copyOfRange(offset, offset + 8)).toLong()

    // ─── 回调接口 ────────────────────────────────────────────────────────────

    interface ServiceCallbacks {
        fun onLoadNewRotation()
        fun onErrorDisconnect()
    }
}
