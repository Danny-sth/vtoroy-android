package com.duq.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duq.android.audio.PlaybackInfo
import com.duq.android.audio.PlaybackState
import com.duq.android.data.model.Message
import com.duq.android.ui.theme.DuqColors

/**
 * List of chat messages with audio playback support.
 *
 * @param messages List of messages to display
 * @param isLoading Whether messages are loading
 * @param audioPlaybackInfo Current audio playback info (which message is playing, progress, etc.)
 * @param onAudioPlayPauseClick Callback when play/pause is clicked for a message
 * @param modifier Modifier
 */
@Composable
fun MessagesList(
    messages: List<Message>,
    isLoading: Boolean = false,
    audioPlaybackInfo: PlaybackInfo = PlaybackInfo(),
    onAudioPlayPauseClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && messages.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = DuqColors.primary
            )
        } else if (messages.isEmpty()) {
            Text(
                text = "No messages yet\nSay \"Hey Duq\" to start",
                color = DuqColors.textTertiary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    // Determine audio state for this specific message
                    val isCurrentlyPlaying = audioPlaybackInfo.messageId == message.id
                    val audioState = if (isCurrentlyPlaying) {
                        when (audioPlaybackInfo.state) {
                            PlaybackState.LOADING -> AudioPlaybackState.LOADING
                            PlaybackState.PLAYING -> AudioPlaybackState.PLAYING
                            PlaybackState.PAUSED -> AudioPlaybackState.PAUSED
                            PlaybackState.IDLE -> AudioPlaybackState.IDLE
                        }
                    } else {
                        AudioPlaybackState.IDLE
                    }
                    val progress = if (isCurrentlyPlaying) audioPlaybackInfo.progress else 0f

                    MessageBubble(
                        message = message,
                        modifier = Modifier.fillMaxWidth(),
                        audioPlaybackState = audioState,
                        audioProgress = progress,
                        onAudioPlayPauseClick = { onAudioPlayPauseClick(message.id) }
                    )
                }
            }
        }
    }
}
