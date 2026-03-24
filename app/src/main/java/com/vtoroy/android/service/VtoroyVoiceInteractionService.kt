package com.vtoroy.android.service

import android.service.voice.VoiceInteractionService
import android.util.Log

class VtoroyVoiceInteractionService : VoiceInteractionService() {

    companion object {
        private const val TAG = "VtoroyVoiceService"
    }

    override fun onReady() {
        super.onReady()
        Log.d(TAG, "Voice interaction service ready")
        // Don't auto-start listener - only works when app is open
    }
}
