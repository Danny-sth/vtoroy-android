package com.vtoroy.android.service

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log

class VtoroyVoiceInteractionSessionService : VoiceInteractionSessionService() {

    companion object {
        private const val TAG = "VtoroySessionService"
    }

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.d(TAG, "New voice interaction session")
        return VtoroyVoiceInteractionSession(this)
    }
}

class VtoroyVoiceInteractionSession(context: android.content.Context) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "VtoroySession"
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "Voice session shown")
    }

    override fun onHide() {
        super.onHide()
        Log.d(TAG, "Voice session hidden")
    }
}
