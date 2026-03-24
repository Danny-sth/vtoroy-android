package com.vtoroy.android.network

import java.io.File

interface VoiceApiClientInterface {
    suspend fun sendVoiceCommand(
        serverUrl: String,
        authToken: String,
        audioFile: File
    ): VtoroyApiClient.ApiResult
}
