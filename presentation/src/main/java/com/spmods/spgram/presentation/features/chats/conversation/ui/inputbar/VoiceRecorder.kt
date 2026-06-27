package com.spmods.spgram.presentation.features.chats.conversation.ui.inputbar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.log10

@Composable
fun rememberVoiceRecorder(
    onRecordingFinished: (String, Int, ByteArray, Boolean) -> Unit,
    onPermissionDenied: () -> Unit = {}
): VoiceRecorderState {
    val context = LocalContext.current
    val state = remember { VoiceRecorderState(context) }

    LaunchedEffect(onRecordingFinished) {
        state.onRecordingFinished = onRecordingFinished
    }

    LaunchedEffect(onPermissionDenied) {
        state.onPermissionDenied = onPermissionDenied
    }

    DisposableEffect(Unit) {
        onDispose {
            state.stopRecording(cancel = true)
        }
    }

    LaunchedEffect(state.isRecording, state.isPaused) {
        if (state.isRecording && !state.isPaused) {
            state.runUpdateLoop()
        }
    }

    return state
}

class VoiceRecorderState(private val context: Context) {
    var isRecording by mutableStateOf(false)
        private set
    var isLocked by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var isViewOnce by mutableStateOf(false)
        private set
    var durationMillis by mutableLongStateOf(0L)
        private set
    var amplitude by mutableFloatStateOf(0f)
        private set

    val waveform = mutableStateListOf<Byte>()

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime = 0L
    private var accumulatedMillis = 0L

    var onRecordingFinished: ((String, Int, ByteArray, Boolean) -> Unit)? = null
    var onPermissionDenied: (() -> Unit)? = null

    fun toggleViewOnce() {
        if (isLocked) {
            isViewOnce = !isViewOnce
        }
    }

    fun togglePause() {
        if (!isRecording || !isLocked) return
        if (isPaused) {
            resumeRecording()
        } else {
            pauseRecording()
        }
    }

    private fun pauseRecording() {
        if (!isRecording || isPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                accumulatedMillis += System.currentTimeMillis() - startTime
                isPaused = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resumeRecording() {
        if (!isRecording || !isPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                startTime = System.currentTimeMillis()
                isPaused = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("DEPRECATION")
    fun startRecording() {
        if (isRecording) return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionDenied?.invoke()
            return
        }

        try {
            val supportsOggOpus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            val extension = if (supportsOggOpus) "ogg" else "m4a"

            val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.$extension")
            currentFile = file
            waveform.clear()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)

                if (supportsOggOpus) {
                    setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                    setAudioEncodingBitRate(320000)
                    setAudioSamplingRate(48000)
                } else {
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(320000)
                    setAudioSamplingRate(48000)
                }

                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            startTime = System.currentTimeMillis()
            accumulatedMillis = 0L
            isRecording = true
            isLocked = false
            isPaused = false
            isViewOnce = false
            durationMillis = 0
        } catch (e: Exception) {
            e.printStackTrace()
            releaseResources()
            isRecording = false
        }
    }

    private fun releaseResources() {
        mediaRecorder?.let { recorder ->
            try {
                recorder.stop()
            } catch (e: Exception) {
                // Ignore
            } finally {
                try {
                    recorder.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        mediaRecorder = null
    }

    fun lockRecording() {
        if (isRecording) {
            isLocked = true
        }
    }

    fun stopRecording(cancel: Boolean = false) {
        if (!isRecording) return

        val capturedDurationMillis = durationMillis
        val capturedIsViewOnce = isViewOnce
        val wasRecording = isRecording
        val file = currentFile

        releaseResources()
        isRecording = false
        isLocked = false
        isPaused = false
        isViewOnce = false
        currentFile = null
        accumulatedMillis = 0L

        if (wasRecording && !cancel && file != null) {
            val durationSec = (capturedDurationMillis / 1000).toInt()
            if (durationSec >= 1) {
                onRecordingFinished?.invoke(file.absolutePath, durationSec, waveform.toByteArray(), capturedIsViewOnce)
            } else {
                file.delete()
            }
        } else {
            file?.delete()
        }
    }

    suspend fun runUpdateLoop() {
        while (isRecording && !isPaused) {
            durationMillis = accumulatedMillis + (System.currentTimeMillis() - startTime)

            val maxAmp = try {
                mediaRecorder?.maxAmplitude ?: 0
            } catch (e: Exception) {
                0
            }

            amplitude = if (maxAmp > 0) {
                (20 * log10(maxAmp.toDouble() / 32767.0)).toFloat().coerceIn(-60f, 0f)
            } else -60f

            val normalized = ((amplitude + 60) / 60 * 31).toInt().coerceIn(0, 31)
            waveform.add(normalized.toByte())

            delay(100)
        }
    }
}
