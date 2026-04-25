package com.duq.android.network

import com.duq.android.config.AppConfig

/**
 * Interface for conversation-related API operations.
 * Decouples ConversationRepository from DuqApiClient implementation.
 */
interface ConversationApiClient {
    /**
     * Get list of user's conversations.
     */
    suspend fun getConversations(authToken: String): Result<List<ConversationResponse>>

    /**
     * Get messages for a conversation.
     */
    suspend fun getMessages(
        authToken: String,
        conversationId: String,
        limit: Int
    ): Result<List<MessageResponse>>

    /**
     * Create a new conversation.
     */
    suspend fun createConversation(
        authToken: String,
        title: String?
    ): Result<ConversationResponse>

    /**
     * Download audio for a message.
     */
    suspend fun downloadAudio(
        authToken: String,
        messageId: Long
    ): Result<ByteArray>
}
