package com.jarvis.android.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(
    private val context: Context,
    private val vad: VoiceActivityDetectorInterface
) : AudioRecorderInterface {

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_RECORDING_MS = 10_000L // Max 10 seconds
        private const val MIN_RECORDING_MS = 1_000L  // Min 1 second
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    override suspend fun recordUntilSilence(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "No RECORD_AUDIO permission")
            return@withContext false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return@withContext false
            }

            val rawFile = File(context.cacheDir, "temp_raw.pcm")
            var totalBytesWritten = 0

            FileOutputStream(rawFile).use { outputStream ->
                audioRecord?.startRecording()
                isRecording = true
                vad.startRecording()

                val buffer = ShortArray(bufferSize / 2)
                val recordingStartTime = System.currentTimeMillis()

                Log.d(TAG, "Started recording (max ${MAX_RECORDING_MS}ms)")

                while (isRecording && isActive) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    val elapsedMs = System.currentTimeMillis() - recordingStartTime

                    if (readSize > 0) {
                        val byteBuffer = ByteArray(readSize * 2)
                        for (i in 0 until readSize) {
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = (buffer[i].toInt() shr 8 and 0xFF).toByte()
                        }
                        outputStream.write(byteBuffer)
                        totalBytesWritten += byteBuffer.size

                        // Check VAD for silence (only after min recording time)
                        if (elapsedMs >= MIN_RECORDING_MS && vad.processAudioBuffer(buffer, readSize)) {
                            Log.d(TAG, "Silence detected after ${elapsedMs}ms, stopping recording")
                            break
                        }
                    }

                    // Check max recording time
                    if (elapsedMs >= MAX_RECORDING_MS) {
                        Log.d(TAG, "Max recording time reached (${MAX_RECORDING_MS}ms), stopping")
                        break
                    }
                }
            }

            stopRecordingInternal()

            Log.d(TAG, "Recording stopped, total bytes: $totalBytesWritten")

            if (totalBytesWritten > 0) {
                convertPcmToWav(rawFile, outputFile, SAMPLE_RATE, 1, 16)
                rawFile.delete()
                return@withContext true
            }

            rawFile.delete()
            return@withContext false

        } catch (e: Exception) {
            Log.e(TAG, "Error recording audio", e)
            stopRecordingInternal()
            return@withContext false
        }
    }

    override fun stopRecording() {
        isRecording = false
        vad.stopRecording()
    }

    private fun stopRecordingInternal() {
        try {
            isRecording = false
            vad.stopRecording()
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    private fun convertPcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val pcmData = pcmFile.readBytes()
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        FileOutputStream(wavFile).use { out ->
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalDataLen))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16))
            out.write(shortToByteArray(1))
            out.write(shortToByteArray(channels.toShort()))
            out.write(intToByteArray(sampleRate))
            out.write(intToByteArray(byteRate))
            out.write(shortToByteArray((channels * bitsPerSample / 8).toShort()))
            out.write(shortToByteArray(bitsPerSample.toShort()))
            out.write("data".toByteArray())
            out.write(intToByteArray(pcmData.size))
            out.write(pcmData)
        }

        Log.d(TAG, "Converted to WAV: ${wavFile.length()} bytes")
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 24 and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            (value.toInt() shr 8 and 0xFF).toByte()
        )
    }
}
