package com.jarvis.android.network

import android.util.Base64
import android.util.Log
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
            val jsonBody = JSONObject().apply {
                put("code", code)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/api/auth/qr/verify")
                .post(requestBody)
                .build()

            Log.d(TAG, "Verifying QR code: $code")

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)

                AuthResult(
                    success = true,
                    token = json.getString("token"),
                    telegramId = json.getLong("telegram_id"),
                    expiresAt = json.getString("expires_at")
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "QR verification failed: ${response.code} - $errorBody")
                AuthResult(success = false, error = errorBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "QR verification error", e)
            AuthResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    override suspend fun sendVoiceCommand(
        serverUrl: String,
        authToken: String,
        audioFile: File
    ): ApiResult = withContext(Dispatchers.IO) {
        try {
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

            Log.d(TAG, "Sending voice command to $url")

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null && body.isNotEmpty()) {
                    val json = JSONObject(body)
                    val text = json.optString("text", "")
                    val audioBase64 = json.optString("audio", "")

                    Log.d(TAG, "Response text: $text")

                    if (audioBase64.isNotEmpty()) {
                        val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                        Log.d(TAG, "Decoded audio: ${audioBytes.size} bytes")
                        ApiResult.Success(audioBytes, text)
                    } else {
                        ApiResult.Error("No audio in response")
                    }
                } else {
                    ApiResult.Error("Empty response from server")
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "API error: ${response.code} - $errorBody")
                ApiResult.Error(errorBody, response.code)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            ApiResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            ApiResult.Error("Error: ${e.message}")
        }
    }
}
