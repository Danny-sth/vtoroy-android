package com.jarvis.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jarvis.android.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AuthReceiver"
        const val ACTION_SET_AUTH = "com.jarvis.android.SET_AUTH"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_TELEGRAM_ID = "telegram_id"
        const val EXTRA_EXPIRES_AT = "expires_at"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SET_AUTH) return

        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return
        val telegramId = intent.getLongExtra(EXTRA_TELEGRAM_ID, 0L)
        val expiresAt = intent.getStringExtra(EXTRA_EXPIRES_AT) ?: ""

        Log.d(TAG, "Received auth: token=${token.take(10)}..., telegramId=$telegramId")

        val settingsRepository = SettingsRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.saveAuthData(token, telegramId, expiresAt)
            Log.d(TAG, "Auth saved successfully")
        }
    }
}
