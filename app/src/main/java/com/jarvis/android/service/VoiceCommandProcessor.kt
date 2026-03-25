package com.jarvis.android.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.jarvis.android.JarvisState
import com.jarvis.android.audio.AudioPlayerInterface
import com.jarvis.android.audio.AudioRecorderInterface
import com.jarvis.android.data.SettingsRepository
import com.jarvis.android.network.JarvisApiClient
import com.jarvis.android.network.VoiceApiClientInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

class VoiceCommandProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorderInterface,
    private val audioPlayer: AudioPlayerInterface,
    private val apiClient: VoiceApiClientInterface,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "VoiceCommandProcessor"
    }

    sealed class ProcessingResult {
        object Success : ProcessingResult()
        data class Error(val message: String) : ProcessingResult()
        object RecordingFailed : ProcessingResult()
    }

    interface StateCallback {
        fun onStateChanged(state: JarvisState)
        fun onError(message: String)
    }

    private suspend fun playListeningBeep() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            delay(200)
            toneGenerator.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play beep", e)
        }
    }

    suspend fun processVoiceCommand(callback: StateCallback): ProcessingResult {
        return try {
            // Play beep to indicate listening started
            playListeningBeep()

            callback.onStateChanged(JarvisState.LISTENING)

            val audioFile = File(context.cacheDir, "voice_command.wav")
            val success = audioRecorder.recordUntilSilence(audioFile)

            if (!success || !audioFile.exists() || audioFile.length() == 0L) {
                Log.e(TAG, "Recording failed")
                return ProcessingResult.RecordingFailed
            }

            Log.d(TAG, "Recording complete: ${audioFile.length()} bytes")

            callback.onStateChanged(JarvisState.PROCESSING)

            val authToken = settingsRepository.authToken.first()
            val result = apiClient.sendVoiceCommand("", authToken, audioFile)

            val processingResult = when (result) {
                is JarvisApiClient.ApiResult.Success -> {
                    Log.d(TAG, "Received response: ${result.audioData.size} bytes")
                    callback.onStateChanged(JarvisState.PLAYING)
                    audioPlayer.playAudio(result.audioData)
                    ProcessingResult.Success
                }
                is JarvisApiClient.ApiResult.Error -> {
                    Log.e(TAG, "API error: ${result.message}")
                    callback.onError(result.message)
                    ProcessingResult.Error(result.message)
                }
            }

            audioFile.delete()
            processingResult
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command", e)
            val errorMessage = "Error: ${e.message}"
            callback.onError(errorMessage)
            ProcessingResult.Error(errorMessage)
        }
    }

    fun stopRecording() {
        audioRecorder.stopRecording()
    }

    fun initializePlayer() {
        audioPlayer.initialize()
    }

    fun releasePlayer() {
        audioPlayer.release()
    }
}
