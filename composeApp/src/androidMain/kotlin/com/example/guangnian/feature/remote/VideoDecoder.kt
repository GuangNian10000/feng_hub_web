package com.example.guangnian.feature.remote

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * H.264 视频解码器，移植自 ScrcpyForAndroid / VideoDecoder.java
 */
internal class VideoDecoder {

    private var mWorker: Worker? = null

    fun decodeSample(data: ByteArray, offset: Int, size: Int, presentationTimeUs: Long, flags: Int) {
        mWorker?.decodeSample(data, offset, size, presentationTimeUs, flags)
    }

    fun configure(surface: Surface, width: Int, height: Int, csd0: ByteBuffer, csd1: ByteBuffer) {
        mWorker?.configure(surface, width, height, csd0, csd1)
    }

    fun start() {
        if (mWorker == null) {
            mWorker = Worker().also {
                it.setRunning(true)
                it.start()
            }
        }
    }

    fun stop() {
        mWorker?.setRunning(false)
        mWorker = null
    }

    private inner class Worker : Thread("ScrcpyVideoWorker") {

        private val mIsRunning = AtomicBoolean(false)
        private val mIsConfigured = AtomicBoolean(false)
        private val sampleQueue = ArrayBlockingQueue<Sample>(30)

        @Volatile
        private var mCodec: MediaCodec? = null

        fun setRunning(running: Boolean) { mIsRunning.set(running) }

        fun configure(surface: Surface, width: Int, height: Int, csd0: ByteBuffer, csd1: ByteBuffer) {
            val csd0Len = csd0.remaining()
            val csd1Len = csd1.remaining()
            if (!surface.isValid || csd0Len <= 0 || csd1Len <= 0) {
                Log.w("ScrcpyVideo", "configure skipped: surface=${surface.isValid} csd0=$csd0Len csd1=$csd1Len")
                return
            }
            if (mIsConfigured.get()) {
                mIsConfigured.set(false)
                mCodec?.stop()
            }
            sampleQueue.clear()
            try {
                val format = MediaFormat.createVideoFormat("video/avc", width, height)
                format.setByteBuffer("csd-0", csd0)
                format.setByteBuffer("csd-1", csd1)
                val codec = MediaCodec.createDecoderByType("video/avc")
                codec.configure(format, surface, null, 0)
                codec.start()
                mCodec = codec
                mIsConfigured.set(true)
                Log.d("ScrcpyVideo", "Video decoder configured: ${width}x${height}")
            } catch (e: Exception) {
                Log.e("ScrcpyVideo", "Video configure failed: ${e.message}", e)
            }
        }

        fun decodeSample(data: ByteArray, offset: Int, size: Int, presentationTimeUs: Long, flags: Int) {
            if (!mIsConfigured.get() || !mIsRunning.get()) return
            val sample = Sample(data.copyOf(), offset, size, presentationTimeUs, flags)
            if (!sampleQueue.offer(sample)) {
                sampleQueue.poll()
                sampleQueue.offer(sample)
            }
        }

        override fun run() {
            try {
                val info = MediaCodec.BufferInfo()
                var pendingSample: Sample? = null
                while (mIsRunning.get()) {
                    val codec = mCodec
                    if (!mIsConfigured.get() || codec == null) {
                        sleep(5)
                        continue
                    }

                    if (pendingSample == null) {
                        pendingSample = sampleQueue.poll()
                    }
                    if (pendingSample != null) {
                        val inputIndex = codec.dequeueInputBuffer(0)
                        if (inputIndex >= 0) {
                            val buf: ByteBuffer? = codec.getInputBuffer(inputIndex)
                            if (buf != null) {
                                buf.put(pendingSample.data, pendingSample.offset, pendingSample.size)
                                codec.queueInputBuffer(
                                    inputIndex, 0, pendingSample.size,
                                    pendingSample.presentationTimeUs, pendingSample.flags
                                )
                                pendingSample = null
                            }
                        } else {
                            sleep(2)
                        }
                    }

                    val outputIndex = codec.dequeueOutputBuffer(info, 0)
                    if (outputIndex >= 0) {
                        codec.releaseOutputBuffer(outputIndex, true)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            } catch (e: IllegalStateException) {
                Log.w("ScrcpyVideo", "Worker IllegalStateException: ${e.message}")
            }
        }
    }

    private data class Sample(
        val data: ByteArray, val offset: Int, val size: Int,
        val presentationTimeUs: Long, val flags: Int
    )
}
