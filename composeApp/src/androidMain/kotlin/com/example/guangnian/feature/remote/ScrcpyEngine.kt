package com.example.guangnian.feature.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

/**
 * ADB 命令执行器 + scrcpy-server 部署器
 * 移植自 ScrcpyForAndroid / App.java + SendCommands.java
 */
object ScrcpyEngine {

    private const val TAG = "ScrcpyEngine"
    private const val ADB_SERVER_PORT = "5137"

    // ─── ADB 命令执行 ──────────────────────────────────────────────────────

    private fun getAdbPath(context: Context): String {
        val srcFile = File("${context.applicationInfo.nativeLibraryDir}/libadb.so")
        if (!srcFile.exists()) {
            Log.e(TAG, "libadb.so not found at: ${srcFile.absolutePath}")
        } else {
            Log.d(TAG, "adb executable: ${srcFile.absolutePath} exists=${srcFile.exists()} exec=${srcFile.canExecute()}")
        }
        return srcFile.absolutePath
    }

    /**
     * 执行 adb 命令并返回 stdout
     */
    fun adbCmd(context: Context, vararg args: String): String {
        val adbPath = getAdbPath(context)
        val cmds = arrayOf(adbPath) + args
        val env = mapOf(
            "HOME" to context.filesDir.absolutePath,
            "TMPDIR" to context.cacheDir.absolutePath,
            "ANDROID_ADB_SERVER_PORT" to ADB_SERVER_PORT
        )
        return runProcess(cmds, env, context.filesDir)
    }

    private fun runProcess(cmds: Array<String>, env: Map<String, String>, workDir: File): String {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            val pb = ProcessBuilder(*cmds).directory(workDir)
            pb.environment().putAll(env)
            pb.redirectErrorStream(true)
            process = pb.start()
            os = DataOutputStream(process.outputStream)

            val sb = StringBuilder()
            val outputThread = Thread {
                try {
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line).append("\n")
                        }
                    }
                } catch (e: Exception) {}
            }
            outputThread.start()

            process?.waitFor()
            // Wait briefly for the output thread to flush, but don't hang if daemon inherited the pipe.
            outputThread.join(200)

            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "runProcess error: ${e.message}")
            ""
        } finally {
            runCatching { os?.close() }
            runCatching { process?.inputStream?.close() }
            runCatching { process?.errorStream?.close() }
            process?.destroy()
        }
    }

    // ─── ADB Server 启动 ───────────────────────────────────────────────────

    fun startAdbServer(context: Context) {
        Thread {
            Log.i(TAG, "Starting ADB server...")
            adbCmd(context, "kill-server")
            adbCmd(context, "start-server")
            Log.i(TAG, "ADB server started")
        }.start()
    }

    // ─── 提取 scrcpy-server.jar ────────────────────────────────────────────

    fun extractServerJar(context: Context): File? {
        return try {
            val scrcpyDir = context.getExternalFilesDir("scrcpy")
                ?: run { Log.e(TAG, "Cannot get external files dir"); return null }
            if (!scrcpyDir.exists()) scrcpyDir.mkdirs()

            val targetFile = File(scrcpyDir, "scrcpy-server.jar")
            context.assets.open("scrcpy-server.jar").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Server jar extracted: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract server jar: ${e.message}", e)
            null
        }
    }

    // ─── 连接 + 部署 + 启动 ────────────────────────────────────────────────

    /**
     * 完整连接流程：
     * 1. adb connect
     * 2. push scrcpy-server.jar 到 /data/local/tmp/
     * 3. 端口转发
     * 4. 启动 scrcpy server
     * 5. 等待 server ready 标记文件
     *
     * @return 0 = 成功，非0 = 失败
     */
    fun connectAndStartServer(
        context: Context,
        serverJar: File,
        ip: String,
        adbPort: Int,
        forwardPort: Int,
        controlPort: Int,
        bitrate: Int,
        maxSize: Int,
        audioEnabled: Boolean,
        localIp: String
    ): Int {
        val targetDevice = "$ip:$adbPort"

        // 1. ADB connect
        val connectRet = adbCmd(context, "connect", targetDevice)
        Log.i(TAG, "adb connect $targetDevice → $connectRet")

        // 2. 验证 jar 文件
        if (!serverJar.exists() || serverJar.length() == 0L) {
            Log.e(TAG, "Server jar not found: ${serverJar.absolutePath}")
            return 2
        }

        // 3. Push jar
        val pushRet = adbCmd(context, "-s", targetDevice, "push", serverJar.absolutePath, "/data/local/tmp/scrcpy-server.jar")
        Log.i(TAG, "adb push → $pushRet")
        if (pushRet.isNullOrEmpty() || !pushRet.contains("file pushed")) {
            Log.e(TAG, "Push failed: $pushRet")
            return 2
        }

        // 4. 杀死旧进程
        adbCmd(context, "-s", targetDevice, "shell", "pkill", "-9", "-f", "xyz.server.scrcpy")
        val killCmd = "for pid in \$(ls /proc 2>/dev/null | grep -E '^[0-9]+$'); do " +
                "if cat /proc/\$pid/cmdline 2>/dev/null | tr '\\0' ' ' | grep -q 'xyz.server.scrcpy'; then " +
                "kill -9 \$pid 2>/dev/null; fi; done"
        adbCmd(context, "-s", targetDevice, "shell", "sh", "-c", killCmd)
        Thread.sleep(1000)

        // 5. 清理旧标记
        adbCmd(context, "-s", targetDevice, "shell", "rm", "-f",
            "/data/local/tmp/scrcpy.ready",
            "/data/local/tmp/scrcpy.started",
            "/data/local/tmp/scrcpy.shell",
            "/data/local/tmp/scrcpy.log")

        // 6. 端口转发
        val fwd1 = adbCmd(context, "-s", targetDevice, "forward", "tcp:$forwardPort", "tcp:7007")
        val fwd2 = adbCmd(context, "-s", targetDevice, "forward", "tcp:$controlPort", "tcp:7008")
        Log.i(TAG, "forward: $fwd1 / $fwd2")

        // 7. 启动 server
        val ipArg = "/$localIp"
        val startCmd = "echo shell_start >/data/local/tmp/scrcpy.shell; " +
                "export CLASSPATH=/data/local/tmp/scrcpy-server.jar; " +
                "/system/bin/app_process / xyz.server.scrcpy.Server " +
                "$ipArg $maxSize $bitrate false $audioEnabled " +
                ">/data/local/tmp/scrcpy.log 2>&1 &"
        adbCmd(context, "-s", targetDevice, "shell", "sh", "-c", startCmd)
        Log.i(TAG, "Server start command sent")

        // 8. 等待 ready 标记
        repeat(100) {
            val readyRet = adbCmd(context, "-s", targetDevice, "shell", "ls", "/data/local/tmp/scrcpy.ready")
            if (readyRet.isNotEmpty() && readyRet.contains("scrcpy.ready")) {
                Log.i(TAG, "Server is ready!")
                return 0
            }
            Thread.sleep(100)
        }

        // 超时，打印日志
        val serverLog = adbCmd(context, "-s", targetDevice, "shell", "sh", "-c",
            "if [ -f /data/local/tmp/scrcpy.log ]; then cat /data/local/tmp/scrcpy.log; fi")
        Log.e(TAG, "Server ready timeout. Log:\n$serverLog")
        return 2
    }
}
