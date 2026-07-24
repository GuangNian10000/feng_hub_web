package com.example.guangnian.feature.remote

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AAC 音频解码器，移植自 ScrcpyForAndroid / AudioDecoder.java
 */
internal class AudioDecoder {

    private var mWorker: Worker? = null

    fun decodeSample(data: ByteArray, offset: Int, size: Int, presentationTimeUs: Long, flags: Int) {
        mWorker?.decodeSample(data, offset, size, presentationTimeUs, flags)
    }

    fun configure(csd: ByteArray) {
        mWorker?.configure(csd)
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

    private inner class Worker : Thread("ScrcpyAudioWorker") {

        private val mIsRunning = AtomicBoolean(false)
        private val mIsConfigured = AtomicBoolean(false)
        private val sampleQueue = ArrayBlockingQueue<Sample>(30)

        @Volatile
        private var mCodec: MediaCodec? = null

        @Volatile
        private var mAudioTrack: AudioTrack? = null

        fun setRunning(running: Boolean) { mIsRunning.set(running) }

        fun configure(csd: ByteArray) {
            if (mIsConfigured.get()) {
                mIsConfigured.set(false)
                mCodec?.stop()
            }
            sampleQueue.clear()
            try {
                val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2)
                format.setByteBuffer("csd-0", ByteBuffer.wrap(csd))
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, 2)

                val codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                codec.configure(format, null, null, 0)
                codec.start()
                mCodec = codec

                val bufferSize = AudioTrack.getMinBufferSize(
                    44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC, 44100,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize, AudioTrack.MODE_STREAM
                )
                track.play()
                mAudioTrack = track

                mIsConfigured.set(true)
                Log.d("ScrcpyAudio", "Audio decoder configured")
            } catch (e: Exception) {
                Log.e("ScrcpyAudio", "Audio configure failed: ${e.message}", e)
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
                        val outBuf: ByteBuffer? = codec.getOutputBuffer(outputIndex)
                        val track = mAudioTrack
                        if (outBuf != null && track != null) {
                            val chunk = ByteArray(info.size)
                            outBuf.get(chunk)
                            track.write(chunk, 0, chunk.size)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScrcpyAudio", "Worker error: ${e.message}")
            } finally {
                mAudioTrack?.stop()
                mAudioTrack?.release()
                mAudioTrack = null
            }
        }
    }

    private data class Sample(
        val data: ByteArray, val offset: Int, val size: Int,
        val presentationTimeUs: Long, val flags: Int
    )
}
