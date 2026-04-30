package com.duq.android.network

import com.google.gson.annotations.SerializedName

/**
 * API response models for backend communication
 */

data class ConversationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("started_at") val startedAt: Long,
    @SerializedName("last_message_at") val lastMessageAt: Long,
    @SerializedName("is_active") val isActive: Boolean
)

data class MessageResponse(
    @SerializedName("id") val id: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    @SerializedName("has_audio") val hasAudio: Boolean = false,
    @SerializedName("audio_duration_ms") val audioDurationMs: Int? = null,
    @SerializedName("waveform") val waveform: List<Float>? = null,
    @SerializedName("created_at") val createdAt: Long
)

data class CreateConversationRequest(
    @SerializedName("title") val title: String?
)

// Unified Message API (POST /api/message)
data class MessageApiRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("message") val message: String,
    @SerializedName("is_voice") val isVoice: Boolean = false,
    @SerializedName("voice_data") val voiceData: String? = null,  // Base64 encoded audio
    @SerializedName("voice_format") val voiceFormat: String? = null,  // wav, ogg, mp3
    @SerializedName("source") val source: String = "android"
)

data class MessageApiResponse(
    @SerializedName("task_id") val taskId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("error") val error: String?
)

// Task Status API (GET /api/task/{id})
data class TaskStatusResponse(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("status") val status: String,
    @SerializedName("response") val response: TaskResponseData?,
    @SerializedName("error") val error: String?
)

data class TaskResponseData(
    @SerializedName("text") val text: String?,
    @SerializedName("audio") val audio: String?,  // Base64 encoded audio
    @SerializedName("audio_format") val audioFormat: String?
)
