package com.jarvis.android.network

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class JarvisApiClient : VoiceApiClientInterface {

    companion object {
        private const val TAG = "JarvisApiClient"
        const val BASE_URL = "https://on-za-menya.online"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    sealed class ApiResult {
        data class Success(val audioData: ByteArray, val text: String = "") : ApiResult() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Success
                return audioData.contentEquals(other.audioData) && text == other.text
            }

            override fun hashCode(): Int = audioData.contentHashCode() + text.hashCode()
        }
        data class Error(val message: String, val code: Int? = null) : ApiResult()
    }

    data class AuthResult(
        val success: Boolean,
        val token: String = "",
        val telegramId: Long = 0,
        val expiresAt: String = "",
        val error: String = ""
    )

    suspend fun verifyQrCode(code: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔐 QR CODE VERIFICATION START")
            Log.d(TAG, "Code: $code")
            Log.d(TAG, "URL: $BASE_URL/api/auth/qr/verify")

            val jsonBody = JSONObject().apply {
                put("code", code)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/api/auth/qr/verify")
                .post(requestBody)
                .build()

            Log.d(TAG, "📤 Sending request...")
            val startTime = System.currentTimeMillis()

            val response = client.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime

            Log.d(TAG, "📥 Response received in ${duration}ms")
            Log.d(TAG, "Status: ${response.code} ${response.message}")

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d(TAG, "Response body length: ${body.length} chars")

                val json = JSONObject(body)
                val token = json.getString("token")
                val telegramId = json.getLong("telegram_id")
                val expiresAt = json.getString("expires_at")

                Log.d(TAG, "✅ QR VERIFICATION SUCCESS")
                Log.d(TAG, "Token: ${token.take(20)}...")
                Log.d(TAG, "Telegram ID: $telegramId")
                Log.d(TAG, "Expires: $expiresAt")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                AuthResult(
                    success = true,
                    token = token,
                    telegramId = telegramId,
                    expiresAt = expiresAt
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "❌ QR VERIFICATION FAILED")
                Log.e(TAG, "HTTP ${response.code}: ${response.message}")
                Log.e(TAG, "Error: $errorBody")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                AuthResult(success = false, error = errorBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ QR VERIFICATION EXCEPTION")
            Log.e(TAG, "Error: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            AuthResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    override suspend fun sendVoiceCommand(
        serverUrl: String,
        authToken: String,
        audioFile: File
    ): ApiResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🎤 VOICE COMMAND START")
            Log.d(TAG, "Audio file: ${audioFile.name}")
            Log.d(TAG, "File size: ${audioFile.length()} bytes (${audioFile.length() / 1024}KB)")
            Log.d(TAG, "URL: $BASE_URL/api/voice")
            Log.d(TAG, "Auth token: ${authToken.take(20)}...")

            val mediaType = "audio/wav".toMediaType()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "audio",
                    audioFile.name,
                    audioFile.asRequestBody(mediaType)
                )
                .build()

            val url = "$BASE_URL/api/voice"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBody)
                .build()

            Log.d(TAG, "📤 Uploading audio...")
            val startTime = System.currentTimeMillis()

            val response = client.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime

            Log.d(TAG, "📥 Response received in ${duration}ms")
            Log.d(TAG, "Status: ${response.code} ${response.message}")

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null && body.isNotEmpty()) {
                    Log.d(TAG, "Response body length: ${body.length} chars")

                    val json = JSONObject(body)
                    val text = json.optString("text", "")
                    val audioBase64 = json.optString("audio", "")

                    Log.d(TAG, "Response text: \"${text.take(100)}${if (text.length > 100) "..." else ""}\"")
                    Log.d(TAG, "Audio base64 length: ${audioBase64.length} chars")

                    if (audioBase64.isNotEmpty()) {
                        val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                        Log.d(TAG, "✅ VOICE COMMAND SUCCESS")
                        Log.d(TAG, "Decoded audio: ${audioBytes.size} bytes (${audioBytes.size / 1024}KB)")
                        Log.d(TAG, "Total time: ${duration}ms")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        ApiResult.Success(audioBytes, text)
                    } else {
                        Log.e(TAG, "❌ No audio in response")
                        Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        ApiResult.Error("No audio in response")
                    }
                } else {
                    Log.e(TAG, "❌ Empty response from server")
                    Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    ApiResult.Error("Empty response from server")
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "❌ VOICE COMMAND FAILED")
                Log.e(TAG, "HTTP ${response.code}: ${response.message}")
                Log.e(TAG, "Error: $errorBody")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                ApiResult.Error(errorBody, response.code)
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ NETWORK ERROR")
            Log.e(TAG, "Error: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            ApiResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ VOICE COMMAND EXCEPTION")
            Log.e(TAG, "Error: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            ApiResult.Error("Error: ${e.message}")
        }
    }

    /**
     * Get list of user's conversations
     */
    suspend fun getConversations(authToken: String): Result<List<ConversationResponse>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📋 Getting conversations list")

                val request = Request.Builder()
                    .url("$BASE_URL/api/conversations")
                    .addHeader("Authorization", "Bearer $authToken")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val listType = object : TypeToken<List<ConversationResponse>>() {}.type
                    val conversations = gson.fromJson<List<ConversationResponse>>(body, listType)
                    Log.d(TAG, "✅ Got ${conversations.size} conversations")
                    Result.success(conversations)
                } else {
                    val error = response.body?.string() ?: "Failed to get conversations"
                    Log.e(TAG, "❌ Get conversations failed: HTTP ${response.code}")
                    Result.failure(IOException(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Get conversations exception: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Get messages for a conversation
     */
    suspend fun getMessages(
        authToken: String,
        conversationId: String,
        limit: Int = 50
    ): Result<List<MessageResponse>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💬 Getting messages for conversation $conversationId")

            val request = Request.Builder()
                .url("$BASE_URL/api/conversations/$conversationId/messages?limit=$limit")
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val listType = object : TypeToken<List<MessageResponse>>() {}.type
                val messages = gson.fromJson<List<MessageResponse>>(body, listType)
                Log.d(TAG, "✅ Got ${messages.size} messages")
                Result.success(messages)
            } else {
                val error = response.body?.string() ?: "Failed to get messages"
                Log.e(TAG, "❌ Get messages failed: HTTP ${response.code}")
                Result.failure(IOException(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get messages exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Create a new conversation
     */
    suspend fun createConversation(
        authToken: String,
        title: String? = null
    ): Result<ConversationResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "➕ Creating new conversation")

            val requestJson = gson.toJson(CreateConversationRequest(title))
            val requestBody = requestJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/api/conversations")
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val conversation = gson.fromJson(body, ConversationResponse::class.java)
                Log.d(TAG, "✅ Created conversation: ${conversation.id}")
                Result.success(conversation)
            } else {
                val error = response.body?.string() ?: "Failed to create conversation"
                Log.e(TAG, "❌ Create conversation failed: HTTP ${response.code}")
                Result.failure(IOException(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Create conversation exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Download audio for a message
     */
    suspend fun downloadAudio(
        authToken: String,
        messageId: Long
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔊 Downloading audio for message $messageId")

            val request = Request.Builder()
                .url("$BASE_URL/api/messages/$messageId/audio")
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val audioBytes = response.body?.bytes() ?: ByteArray(0)
                Log.d(TAG, "✅ Downloaded ${audioBytes.size} bytes")
                Result.success(audioBytes)
            } else {
                val error = response.body?.string() ?: "Failed to download audio"
                Log.e(TAG, "❌ Download audio failed: HTTP ${response.code}")
                Result.failure(IOException(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Download audio exception: ${e.message}")
            Result.failure(e)
        }
    }
}
