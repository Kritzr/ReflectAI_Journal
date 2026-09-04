package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File

class AudioRecordingManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false
    private var startTimeMillis = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var amplitudeCallback: ((Float, Long) -> Unit)? = null

    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val maxAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)
                val elapsed = System.currentTimeMillis() - startTimeMillis
                amplitudeCallback?.invoke(normalizedAmp, elapsed)
                handler.postDelayed(this, 100)
            }
        }
    }

    fun startRecording(onAmplitudeUpdate: (Float, Long) -> Unit): Boolean {
        if (isRecording) return false

        return try {
            val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            startTimeMillis = System.currentTimeMillis()
            amplitudeCallback = onAmplitudeUpdate
            handler.post(amplitudeRunnable)
            Log.d("AudioRecordingManager", "Started recording to ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Failed to start recording: ${e.message}", e)
            cleanup()
            false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null

        handler.removeCallbacks(amplitudeRunnable)
        isRecording = false
        amplitudeCallback = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Error stopping MediaRecorder: ${e.message}", e)
        } finally {
            mediaRecorder = null
        }

        val file = currentOutputFile
        if (file != null && file.exists() && file.length() > 0) {
            Log.d("AudioRecordingManager", "Recording saved: ${file.absolutePath} (${file.length()} bytes)")
            return file
        }
        return null
    }

    fun cancelRecording() {
        handler.removeCallbacks(amplitudeRunnable)
        isRecording = false
        amplitudeCallback = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore cancel exceptions
        } finally {
            mediaRecorder = null
        }

        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
    }

    private fun cleanup() {
        handler.removeCallbacks(amplitudeRunnable)
        isRecording = false
        amplitudeCallback = null
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
    }
}
