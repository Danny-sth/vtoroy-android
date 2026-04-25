package com.duq.android.audio

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import com.duq.android.config.AppConfig
import kotlin.coroutines.resume

class AudioPlayer(private val context: Context) : AudioPlayerInterface {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    private var exoPlayer: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun initialize() {
        mainHandler.post {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(context).build()
            }
        }
    }

    override suspend fun playAudio(audioData: ByteArray): Boolean = suspendCancellableCoroutine { continuation ->
        // Validate input
        if (audioData.isEmpty()) {
            Log.w(TAG, "Empty audio data, skipping playback")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        // Track current listener for cleanup
        var currentListener: Player.Listener? = null

        try {
            val tempFile = File(context.cacheDir, AppConfig.RESPONSE_AUDIO_FILENAME)
            FileOutputStream(tempFile).use { it.write(audioData) }

            mainHandler.post {
                try {
                    val player = exoPlayer ?: ExoPlayer.Builder(context).build().also { exoPlayer = it }

                    // Create listener with proper cleanup
                    val listener = object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_ENDED -> {
                                    Log.d(TAG, "Playback ended")
                                    cleanup(player, this)
                                    if (continuation.isActive) {
                                        continuation.resume(true)
                                    }
                                }
                                Player.STATE_IDLE -> Log.d(TAG, "Player idle")
                                Player.STATE_BUFFERING -> Log.d(TAG, "Buffering")
                                Player.STATE_READY -> Log.d(TAG, "Ready to play")
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e(TAG, "Playback error", error)
                            cleanup(player, this)
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }

                    currentListener = listener
                    player.addListener(listener)

                    val mediaItem = MediaItem.fromUri(Uri.fromFile(tempFile))
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()

                    Log.d(TAG, "Started playback")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in main thread playback", e)
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            // Proper cleanup on coroutine cancellation
            continuation.invokeOnCancellation {
                mainHandler.post {
                    exoPlayer?.let { player ->
                        currentListener?.let { listener ->
                            cleanup(player, listener)
                        }
                        player.stop()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    /**
     * Cleanup listener from player to prevent memory leaks.
     */
    private fun cleanup(player: ExoPlayer, listener: Player.Listener) {
        try {
            player.removeListener(listener)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing listener: ${e.message}")
        }
    }

    override fun stop() {
        mainHandler.post {
            exoPlayer?.stop()
        }
    }

    override fun release() {
        mainHandler.post {
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
