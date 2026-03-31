package com.jarvis.android.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.jarvis.android.JarvisState
import com.jarvis.android.audio.AudioPlayerInterface
import com.jarvis.android.audio.AudioRecorderInterface
import com.jarvis.android.data.ConversationRepository
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
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository
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
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🎙️ VOICE COMMAND PROCESSING START")

            // Play beep to indicate listening started
            Log.d(TAG, "Playing listening beep...")
            playListeningBeep()
            Log.d(TAG, "Beep completed")

            Log.d(TAG, "→ STATE: LISTENING")
            callback.onStateChanged(JarvisState.LISTENING)

            val audioFile = File(context.cacheDir, "voice_command.wav")
            Log.d(TAG, "Recording to: ${audioFile.absolutePath}")

            val recordingStartTime = System.currentTimeMillis()
            val success = audioRecorder.recordUntilSilence(audioFile)
            val recordingDuration = System.currentTimeMillis() - recordingStartTime

            if (!success || !audioFile.exists() || audioFile.length() == 0L) {
                Log.e(TAG, "❌ RECORDING FAILED")
                Log.e(TAG, "Success: $success, Exists: ${audioFile.exists()}, Size: ${audioFile.length()}")
                Log.e(TAG, "═══════════════════════════════════════")
                return ProcessingResult.RecordingFailed
            }

            Log.d(TAG, "✅ Recording complete")
            Log.d(TAG, "Duration: ${recordingDuration}ms")
            Log.d(TAG, "File size: ${audioFile.length()} bytes (${audioFile.length() / 1024}KB)")

            Log.d(TAG, "→ STATE: PROCESSING")
            callback.onStateChanged(JarvisState.PROCESSING)

            Log.d(TAG, "Getting auth token...")
            val authToken = settingsRepository.authToken.first()
            Log.d(TAG, "Token retrieved: ${authToken.take(20)}...")

            Log.d(TAG, "Sending voice command to API...")
            val result = apiClient.sendVoiceCommand("", authToken, audioFile)

            val processingResult = when (result) {
                is JarvisApiClient.ApiResult.Success -> {
                    Log.d(TAG, "✅ API SUCCESS")
                    Log.d(TAG, "Response audio: ${result.audioData.size} bytes")
                    Log.d(TAG, "Response text: ${result.text.take(100)}${if (result.text.length > 100) "..." else ""}")

                    Log.d(TAG, "→ STATE: PLAYING")
                    callback.onStateChanged(JarvisState.PLAYING)

                    Log.d(TAG, "Starting audio playback...")
                    val playbackSuccess = audioPlayer.playAudio(result.audioData)

                    if (playbackSuccess) {
                        Log.d(TAG, "✅ Playback completed successfully")
                    } else {
                        Log.w(TAG, "⚠️ Playback completed with warning")
                    }

                    // Update conversation history from backend
                    try {
                        Log.d(TAG, "Refreshing conversation history from backend...")
                        val conversationId = conversationRepository.getCurrentConversationId(authToken)
                        if (conversationId != null) {
                            conversationRepository.refreshMessages(authToken, conversationId)
                            Log.d(TAG, "✅ Conversation history refreshed")
                        } else {
                            Log.w(TAG, "⚠️ No active conversation to refresh")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to refresh conversation: ${e.message}")
                        // Non-fatal, continue
                    }

                    ProcessingResult.Success
                }
                is JarvisApiClient.ApiResult.Error -> {
                    Log.e(TAG, "❌ API ERROR")
                    Log.e(TAG, "Message: ${result.message}")
                    Log.e(TAG, "Code: ${result.code}")
                    callback.onError(result.message)
                    ProcessingResult.Error(result.message)
                }
            }

            Log.d(TAG, "Cleaning up audio file...")
            audioFile.delete()

            Log.d(TAG, "🏁 VOICE COMMAND PROCESSING COMPLETE")
            Log.d(TAG, "═══════════════════════════════════════")

            processingResult
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION IN VOICE COMMAND PROCESSING")
            Log.e(TAG, "Error: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Log.e(TAG, "═══════════════════════════════════════")
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
