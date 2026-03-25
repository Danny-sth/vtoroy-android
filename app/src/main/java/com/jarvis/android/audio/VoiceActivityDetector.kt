package com.jarvis.android.audio

import android.content.Context
import android.util.Log
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate

class VoiceActivityDetector(
    private val context: Context,
    private val silenceTimeoutMs: Long = 2000L,  // 2 sec of silence to stop
    private val minRecordingMs: Long = 500L      // Min 0.5 sec recording
) : VoiceActivityDetectorInterface {

    companion object {
        private const val TAG = "VoiceActivityDetector"
    }

    private var vadSilero: VadSilero? = null
    private var lastSpeechTime: Long = 0
    private var recordingStartTime: Long = 0
    private var isRecording = false
    private var frameCount = 0

    override fun startRecording() {
        val now = System.currentTimeMillis()
        lastSpeechTime = now
        recordingStartTime = now
        isRecording = true
        frameCount = 0

        try {
            vadSilero?.close()
        } catch (e: Exception) {
            // Ignore
        }

        vadSilero = VadSilero(
            context,
            sampleRate = SampleRate.SAMPLE_RATE_16K,
            frameSize = FrameSize.FRAME_SIZE_512,
            mode = Mode.NORMAL,
            silenceDurationMs = 800,
            speechDurationMs = 100
        )

        Log.d(TAG, "Started VAD with Silero DNN (NORMAL mode)")
    }

    override fun stopRecording() {
        isRecording = false
        try {
            vadSilero?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vadSilero = null
        Log.d(TAG, "Stopped voice activity detection")
    }

    override fun processAudioBuffer(buffer: ShortArray, readSize: Int): Boolean {
        if (!isRecording) return false

        val vad = vadSilero ?: return false
        frameCount++

        // Silero VAD uses 512 samples at 16kHz
        val frameSize = 512
        var speechDetectedInBuffer = false

        var offset = 0
        while (offset + frameSize <= readSize) {
            val frame = buffer.copyOfRange(offset, offset + frameSize)
            try {
                if (vad.isSpeech(frame)) {
                    speechDetectedInBuffer = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "VAD error", e)
            }
            offset += frameSize
        }

        val now = System.currentTimeMillis()

        if (speechDetectedInBuffer) {
            lastSpeechTime = now
        }

        // Log every ~1 second
        if (frameCount % 30 == 0) {
            val silenceDuration = now - lastSpeechTime
            Log.d(TAG, "Frame $frameCount: speech=$speechDetectedInBuffer, silence=${silenceDuration}ms")
        }

        val recordingDuration = now - recordingStartTime

        // Don't stop before minimum recording time
        if (recordingDuration < minRecordingMs) {
            return false
        }

        // Check if silence exceeded timeout
        val silenceDuration = now - lastSpeechTime
        if (silenceDuration >= silenceTimeoutMs) {
            Log.d(TAG, "Silence confirmed after ${silenceDuration}ms (recorded ${recordingDuration}ms)")
            return true
        }

        return false
    }

    override fun reset() {
        lastSpeechTime = System.currentTimeMillis()
        frameCount = 0
    }
}
